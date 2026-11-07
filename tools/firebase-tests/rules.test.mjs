import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';

let environment;
before(async () => {
  environment = await initializeTestEnvironment({
    projectId: 'demo-sportpro',
    firestore: { host: '127.0.0.1', port: 8085, rules: await readFile(new URL('../../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(async () => { await environment?.cleanup(); });
beforeEach(async () => { await environment.clearFirestore(); });

function db(uid = 'alice', claims = {}) {
  return environment.authenticatedContext(uid, { email: uid + '@example.com', ...claims }).firestore();
}
function profile(uid = 'alice', role = 'JUG') {
  return { uid, email: uid + '@example.com', names: 'Ana', lastNames: 'Torres',
    selectedRole: role, birthDateMillis: role === 'JUG' || role === 'PAD' ? 946684800000 : null,
    isActive: true, createdAt: serverTimestamp(), updatedAt: serverTimestamp() };
}
const ref = (database, uid = 'alice') => doc(database, 'users', uid);

for (const role of ['ADM', 'DT', 'JUG', 'PAD']) {
  test('onboarding and own read: ' + role, async () => {
    const database = db();
    await assertSucceeds(setDoc(ref(database), profile('alice', role)));
    await assertSucceeds(getDoc(ref(database)));
  });
}

test('anonymous reads and writes denied', async () => {
  const database = environment.unauthenticatedContext().firestore();
  await assertFails(setDoc(ref(database), profile()));
  await assertFails(getDoc(ref(database)));
  await assertFails(getDocs(collection(database, 'users')));
});
test('another user cannot read private email/birth date', async () => {
  await setDoc(ref(db()), profile());
  await assertFails(getDoc(ref(db('bob'))));
  await assertFails(updateDoc(ref(db('bob')), { names: 'Changed' }));
  await assertFails(deleteDoc(ref(db('bob'))));
});
test('users cannot list profiles', async () => {
  await assertFails(getDocs(collection(db(), 'users')));
});
test('owner cannot change role or activation state', async () => {
  const database = db();
  await setDoc(ref(database), profile());
  await assertFails(updateDoc(ref(database), { selectedRole: 'ADM' }));
  await assertFails(updateDoc(ref(database), { isActive: false }));
  await assertFails(updateDoc(ref(database), { names: 'a'.repeat(5000) }));
  await assertFails(deleteDoc(ref(database)));
});
test('selecting ADM does not grant access to another academy or user', async () => {
  await setDoc(ref(db()), profile('alice', 'ADM'));
  await assertFails(getDoc(ref(db(), 'bob')));
  await assertFails(setDoc(doc(db(), 'academies', 'foreign'), { ownerUid: 'bob' }));
});

for (const [label, changes] of [
  ['foreign UID', { uid: 'bob' }],
  ['wrong email', { email: 'bob@example.com' }],
  ['invalid role', { selectedRole: 'ROOT' }],
  ['empty names', { names: '' }],
  ['oversized names', { names: 'a'.repeat(81) }],
  ['oversized surname', { lastNames: 'a'.repeat(81) }],
  ['extra privileges', { isAdmin: true }],
  ['future birth date', { birthDateMillis: 4102444800000 }],
  ['invalid birth date type', { birthDateMillis: '2000-01-01' }],
  ['missing required birth date', { birthDateMillis: null }],
  ['wrong bool type', { isActive: 'true' }],
  ['forged creation date', { createdAt: new Date(0) }],
  ['wrong timestamp type', { updatedAt: 123 }],
]) {
  test('invalid create denied: ' + label, async () => {
    await assertFails(setDoc(ref(db()), { ...profile(), ...changes }));
  });
}
test('missing required field denied', async () => {
  const data = profile();
  delete data.lastNames;
  await assertFails(setDoc(ref(db()), data));
});
test('cannot create a profile on another UID path', async () => {
  await assertFails(setDoc(ref(db(), 'bob'), profile('bob')));
});
test('imported account can bootstrap only using signed role for null birth date', async () => {
  const data = { ...profile(), birthDateMillis: null, lastNames: '' };
  await assertFails(setDoc(ref(db()), data));
  await assertSucceeds(setDoc(ref(db('alice', { role: 'JUG' })), data));
});
test('verified Auth email can synchronize without changing UID or role', async () => {
  await setDoc(ref(db()), profile());
  const verified = db('alice', { email: 'new@example.com', email_verified: true });
  await assertSucceeds(updateDoc(ref(verified), { email: 'new@example.com', updatedAt: serverTimestamp() }));
  const saved = (await getDoc(ref(verified))).data();
  if (saved.uid !== 'alice' || saved.selectedRole !== 'JUG') throw new Error('Identity changed');
});
test('unverified email, arbitrary email and unrelated profile edits stay denied', async () => {
  await setDoc(ref(db()), profile());
  const unverified = db('alice', { email: 'new@example.com', email_verified: false });
  await assertFails(updateDoc(ref(unverified), { email: 'new@example.com', updatedAt: serverTimestamp() }));
  const verified = db('alice', { email: 'new@example.com', email_verified: true });
  await assertFails(updateDoc(ref(verified), { email: 'attacker@example.com', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(verified), { email: 'new@example.com', selectedRole: 'ADM', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(verified), { email: 'new@example.com', uid: 'bob', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(verified), { email: 'new@example.com', isActive: false, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref(verified), { email: 'new@example.com', updatedAt: new Date(0) }));
});
test('another verified user cannot synchronize someone else email', async () => {
  await setDoc(ref(db()), profile());
  await assertFails(updateDoc(ref(db('bob', { email_verified: true })), {
    email: 'bob@example.com', updatedAt: serverTimestamp(),
  }));
});
test('suspended profile cannot update email', async () => {
  await environment.withSecurityRulesDisabled(async context => {
    await setDoc(ref(context.firestore()), { ...profile(), isActive: false });
  });
  await assertFails(updateDoc(ref(db('alice', { email: 'new@example.com', email_verified: true })), {
    email: 'new@example.com', updatedAt: serverTimestamp(),
  }));
});
test('orphaned subcollections stay closed', async () => {
  await assertFails(setDoc(doc(db(), 'users/alice/privateContacts/emergency'), { phone: '123' }));
  await assertFails(getDoc(doc(db(), 'users/alice/privateContacts/emergency')));
});
