// One-time, explicitly authorized bootstrap. Never included in the Android app.
const auth = require(process.argv[2]);
const project = 'sportpro-dpa-2026-rh';
const uid = 'Sz9FtZwrizdiRfvqMDqV0RKSSzo1';
const expectedEmail = 'admin@gmail.com';

async function main() {
  const account = auth.getProjectDefaultAccount(process.cwd()) || auth.getGlobalDefaultAccount();
  if (!account) throw new Error('Firebase CLI no tiene una sesión administrativa.');
  const credentials = await auth.getAccessToken(account.tokens.refresh_token, [
    'https://www.googleapis.com/auth/cloud-platform',
    'https://www.googleapis.com/auth/firebase'
  ]);
  const token = credentials.access_token;
  if (!token) throw new Error('No se pudo obtener autorización administrativa.');
  async function api(url, method = 'GET', body) {
    const response = await fetch(url, {
      method,
      headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json', 'x-goog-user-project': project },
      ...(body ? { body: JSON.stringify(body) } : {})
    });
    const result = await response.json();
    if (response.status === 404) return null;
    if (!response.ok) throw new Error(result.error?.message || 'HTTP ' + response.status);
    return result;
  }
  const identity = await api('https://identitytoolkit.googleapis.com/v1/projects/' + project + '/accounts:lookup',
    'POST', { localId: [uid] });
  const user = identity?.users?.[0];
  if (!user || user.localId !== uid || user.email !== expectedEmail || user.disabled) {
    throw new Error('UID/correo no coinciden o la cuenta está deshabilitada. No se escribió nada.');
  }
  const base = 'https://firestore.googleapis.com/v1/projects/' + project + '/databases/sportpro-live/documents';
  const profile = await api(base + '/users/' + uid);
  if (profile?.fields?.isActive?.booleanValue !== true) {
    throw new Error('El perfil SportPro no está activo. No se escribió nada.');
  }
  const path = base + '/platformAuthorities/' + uid;
  const existing = await api(path);
  if (existing && existing.fields?.active?.booleanValue !== true) {
    throw new Error('Ya existe una autoridad desactivada. No se sobrescribió.');
  }
  if (!existing) {
    await api(base + '/platformAuthorities?documentId=' + uid, 'POST', {
      fields: { active: { booleanValue: true } }
    });
  }
  const verified = await api(path);
  if (verified?.fields?.active?.booleanValue !== true) throw new Error('No se confirmó la autorización.');
  console.log(JSON.stringify({ email: expectedEmail, uid, platformAuthority: true, verified: true, created: !existing }));
}
main().catch(error => {
  console.error('ERROR: ' + error.message);
  process.exitCode = 1;
});
