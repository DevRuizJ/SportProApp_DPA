// Explicit opt-in integration check against the configured course project.
// Creates ONLY synthetic accounts and cleans up ONLY UIDs created in this process.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { initializeApp, deleteApp } from 'firebase/app';
import { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, signOut, deleteUser } from 'firebase/auth';
import { getFirestore, doc, runTransaction, getDocFromServer, serverTimestamp, terminate } from 'firebase/firestore';

if (!process.argv.includes('--run-live')) throw new Error('Pass --run-live explicitly.');
const config = JSON.parse(await readFile(new URL('../../app/google-services.json', import.meta.url), 'utf8'));
assert.equal(config.project_info.project_id, 'sportpro-dpa-2026-rh');
const client = config.client.find(c => c.client_info.android_client_info.package_name === 'com.dpa.sportpro');
const app = initializeApp({
  apiKey: client.api_key[0].current_key,
  projectId: config.project_info.project_id,
  appId: client.client_info.mobilesdk_app_id,
}, 'live-auth-smoke-' + randomUUID());
const auth = getAuth(app);
const database = getFirestore(app, 'sportpro-live');
const createdUids = new Set();
const failures = [];
try {
  for (const role of ['ADM', 'DT', 'JUG', 'PAD']) {
    const run = randomUUID().replaceAll('-', '');
    const email = 'sportpro-smoke-' + role.toLowerCase() + '-' + run + '@example.com';
    const password = 'Prueba9A-' + randomUUID();
    let uid;
    try {
      const result = await createUserWithEmailAndPassword(auth, email, password);
      uid = result.user.uid;
      createdUids.add(uid);
      const reference = doc(database, 'users', uid);
      await runTransaction(database, async tx => {
        const existing = await tx.get(reference);
        assert.equal(existing.exists(), false);
        tx.set(reference, {
          uid, email, names: 'Prueba', lastNames: 'Automatizada', selectedRole: role,
          birthDateMillis: ['JUG', 'PAD'].includes(role) ? 946684800000 : null,
          isActive: true, createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
        });
      });
      await signOut(auth);
      const login = await signInWithEmailAndPassword(auth, email, password);
      assert.equal(login.user.uid, uid);
      const stored = await getDocFromServer(reference);
      assert.equal(stored.data().selectedRole, role);
      assert.equal(stored.data().names, 'Prueba');
      await assert.rejects(getDocFromServer(doc(database, 'users', 'another-user')), /permission|insufficient/i);
      console.log('PASS real Firebase signup -> profile -> signout -> login: ' + role);
    } catch (error) {
      failures.push(role + ': ' + (error.code || error.message));
      console.error('FAIL ' + role + ': ' + (error.code || error.message));
    } finally {
      if (uid) {
        assert.ok(createdUids.has(uid) && /^[A-Za-z0-9_-]{1,128}$/.test(uid));
        // Admin cleanup is constrained to the synthetic UID created above, never a pre-existing account.
        try {
          execFileSync('cmd.exe', ['/d', '/s', '/c',
            'npx.cmd -y firebase-tools@latest firestore:delete users/' + uid +
            ' --database sportpro-live --project sportpro-dpa-2026-rh --force'],
            { cwd: new URL('../../', import.meta.url), stdio: 'pipe', timeout: 120000 });
          const current = await signInWithEmailAndPassword(auth, email, password);
          assert.equal(current.user.uid, uid);
          await deleteUser(current.user);
          console.log('CLEANED synthetic account and profile: ' + role);
        } catch (error) {
          failures.push('Cleanup ' + uid + ': ' + error.message);
          console.error('Cleanup required for synthetic UID: ' + uid);
        }
      }
    }
  }
} finally {
  await signOut(auth);
  await terminate(database);
  await deleteApp(app);
}
assert.deepEqual(failures, []);
console.log('All four roles passed real Firebase integration and cleanup.');
