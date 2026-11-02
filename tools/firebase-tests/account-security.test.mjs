import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { initializeApp, deleteApp } from 'firebase/app';
import {
  getAuth, connectAuthEmulator, createUserWithEmailAndPassword, signInWithEmailAndPassword,
  signOut, sendPasswordResetEmail, confirmPasswordReset, verifyPasswordResetCode,
  reauthenticateWithCredential, EmailAuthProvider, updatePassword,
  verifyBeforeUpdateEmail, applyActionCode, reload,
} from 'firebase/auth';

// Demo project + explicit localhost emulator. Never sends real email or changes production users.
const projectId = 'demo-sportpro';
const host = process.env.FIREBASE_AUTH_EMULATOR_HOST;
assert.equal(host, '127.0.0.1:9099', 'Run via Firebase Auth Emulator Suite, not production');
const app = initializeApp({ projectId, apiKey: 'demo-api-key' }, 'account-security-tests');
const auth = getAuth(app);
connectAuthEmulator(auth, 'http://' + host, { disableWarnings: true });
after(async () => { await signOut(auth); await deleteApp(app); });

async function codeFor(email, type) {
  const response = await fetch('http://' + host + '/emulator/v1/projects/' + projectId + '/oobCodes');
  assert.equal(response.status, 200);
  const payload = await response.json();
  const code = payload.oobCodes?.findLast(value =>
    value.requestType === type && (value.email === email || value.newEmail === email));
  assert.ok(code, 'Expected emulator-only email action: ' + type);
  return code.oobCode;
}

test('reset link replaces password, preserves UID and cannot be reused', async () => {
  const email = 'reset@example.com';
  const created = await createUserWithEmailAndPassword(auth, email, 'Original123');
  const uid = created.user.uid;
  await signOut(auth);
  await sendPasswordResetEmail(auth, email);
  const code = await codeFor(email, 'PASSWORD_RESET');
  assert.equal(await verifyPasswordResetCode(auth, code), email);
  await confirmPasswordReset(auth, code, 'Restored123');
  await assert.rejects(signInWithEmailAndPassword(auth, email, 'Original123'));
  assert.equal((await signInWithEmailAndPassword(auth, email, 'Restored123')).user.uid, uid);
  await assert.rejects(confirmPasswordReset(auth, code, 'Another123'));
  await signOut(auth);
});

test('wrong current password prevents mutation; reauthentication enables password change', async () => {
  const email = 'password@example.com';
  const { user } = await createUserWithEmailAndPassword(auth, email, 'Original123');
  await assert.rejects(reauthenticateWithCredential(user, EmailAuthProvider.credential(email, 'Wrong123')));
  await signOut(auth);
  assert.equal((await signInWithEmailAndPassword(auth, email, 'Original123')).user.uid, user.uid);
  await reauthenticateWithCredential(auth.currentUser, EmailAuthProvider.credential(email, 'Original123'));
  await updatePassword(auth.currentUser, 'Changed123');
  await signOut(auth);
  await assert.rejects(signInWithEmailAndPassword(auth, email, 'Original123'));
  assert.equal((await signInWithEmailAndPassword(auth, email, 'Changed123')).user.uid, user.uid);
  await signOut(auth);
});

test('email remains unchanged until verification; verified login keeps UID', async () => {
  const oldEmail = 'old-email@example.com';
  const newEmail = 'verified-email@example.com';
  const { user } = await createUserWithEmailAndPassword(auth, oldEmail, 'Original123');
  await reauthenticateWithCredential(user, EmailAuthProvider.credential(oldEmail, 'Original123'));
  await verifyBeforeUpdateEmail(user, newEmail);
  await reload(user);
  assert.equal(user.email, oldEmail);
  await assert.rejects(signInWithEmailAndPassword(auth, newEmail, 'Original123'));
  const code = await codeFor(newEmail, 'VERIFY_AND_CHANGE_EMAIL');
  await applyActionCode(auth, code);
  await signOut(auth);
  await assert.rejects(signInWithEmailAndPassword(auth, oldEmail, 'Original123'));
  const signed = (await signInWithEmailAndPassword(auth, newEmail, 'Original123')).user;
  assert.equal(signed.uid, user.uid);
  assert.equal(signed.emailVerified, true);
  await assert.rejects(applyActionCode(auth, code));
  await signOut(auth);
});
