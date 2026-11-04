import { test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, serverTimestamp,
  runTransaction, onSnapshot, writeBatch } from 'firebase/firestore';

let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-sportpro',
    firestore: { host: '127.0.0.1', port: 8085, rules: await readFile(new URL('../../firestore.rules', import.meta.url), 'utf8') } });
});
after(async () => { await env?.cleanup(); });
const types = ['period_start','period_end','goal','goal_kick','throw_in','corner_kick','foul','yellow_card','red_card','penalty','offside','substitution'];
const db = (uid = 'coach', role = 'DT') => env.authenticatedContext(uid, { email: uid + '@example.com', role: 'authenticated', app_role: role }).firestore();
const matchRef = database => doc(database, 'matches/m1');
const eventRef = (database, id) => doc(database, 'matches/m1/events/' + id);
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    for (const uid of ['coach','other','viewer','outsider','requested-admin']) {
      await setDoc(doc(ctx.firestore(), 'users', uid), { isActive: true, selectedRole: 'ADM' });
    }
  });
});
async function createMatch(database = db()) {
  await setDoc(doc(database, 'matchCatalogs/cat1'), {
    ownerUid: 'coach', category: 'Sub-15', enabled: types, version: 1, updatedAt: serverTimestamp(),
  });
  await runTransaction(database, async tx => {
    const previous = await tx.get(matchRef(database));
    assert.equal(previous.exists(), false);
    tx.set(matchRef(database), {
    ownerUid: 'coach', home: 'Local de prueba', away: 'Visitante de prueba', category: 'Sub-15',
    catalogId: 'cat1', periodMinutes: 30, viewerUids: ['coach','other','viewer'], operatorUids: ['coach','other'],
    phase: 'READY', accumulatedSeconds: 0, startedAt: null, clockVersion: 0, lastClockEvent: '',
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
    });
  });
}
function event(id, operation = id, version = 1, extra = {}) {
  return { eventId: id, operationId: operation, version, typeId: 'goal', seconds: 60, side: 'HOME',
    player: '', otherPlayer: '', detail: 'Jugada registrada', observations: '', voided: false,
    actorUid: 'coach', reason: version > 1 ? 'Corrección observada' : '', recordedAt: serverTimestamp(),
    catalogVersion: 1, capturedAtMillis: Date.now(), ...extra };
}
async function writeEvent(database, data, expected = data.version - 1) {
  const er = eventRef(database, data.eventId);
  const hr = doc(er, 'history', data.operationId);
  return runTransaction(database, async tx => {
    if ((await tx.get(hr)).exists()) return false;
    const old = await tx.get(er);
    if ((old.data()?.version ?? 0) !== expected) throw new Error('VERSION_CONFLICT');
    tx.set(er, data); tx.set(hr, data);
    return true;
  });
}
async function clock(database, phase, accumulated = 0) {
  return runTransaction(database, async tx => {
    const old = (await tx.get(matchRef(database))).data();
    const version = old.clockVersion + 1;
    const running = ['FIRST','SECOND'].includes(phase);
    const boundary = ['READY','HALF_TIME'].includes(old.phase) || ['HALF_TIME','FINISHED'].includes(phase);
    const id = boundary ? 'clock_' + version : '';
    tx.update(matchRef(database), { phase, accumulatedSeconds: accumulated,
      startedAt: running ? serverTimestamp() : null, clockVersion: version, lastClockEvent: id, updatedAt: serverTimestamp() });
    if (boundary) {
      const e = event(id, id, 1, { typeId: running ? 'period_start' : 'period_end',
        seconds: accumulated, side: '', detail: 'Periodo', catalogVersion: 0 });
      tx.set(eventRef(database, id), e); tx.set(doc(eventRef(database, id), 'history', id), e);
    }
  });
}
async function started() { const database = db(); await createMatch(database); await clock(database, 'FIRST'); return database; }

test('a forgotten running clock can close at the supported four-hour limit, never above it', async () => {
  const database = await started();
  await env.withSecurityRulesDisabled(async ctx => {
    await updateDoc(matchRef(ctx.firestore()), { startedAt: new Date(Date.now() - 18_000_000) });
  });
  await assertFails(clock(database, 'HALF_TIME', 18000));
  await assertSucceeds(clock(database, 'HALF_TIME', 14400));
  assert.equal((await getDoc(matchRef(database))).data().accumulatedSeconds, 14400);
  assert.equal((await getDoc(eventRef(database, 'clock_2'))).data().seconds, 14400);
});

test('signed DT creates; selecting ADM in a profile never grants remote privileges', async () => {
  await assertSucceeds(createMatch());
  const requested = env.authenticatedContext('requested-admin', { email: 'requested-admin@example.com' }).firestore();
  await assertFails(setDoc(doc(requested, 'matchCatalogs/evil'), {
    ownerUid: 'requested-admin', category: 'X', enabled: types, version: 1, updatedAt: serverTimestamp(),
  }));
});
test('viewer sees assigned match but cannot operate; another DT has no implicit access', async () => {
  const database = await started();
  await assertSucceeds(getDoc(matchRef(db('viewer','PAD'))));
  await assertFails(getDoc(matchRef(db('outsider','DT'))));
  await assertFails(writeEvent(db('viewer','JUG'), event('bad', 'bad', 1, { actorUid: 'viewer' })));
  await assertFails(updateDoc(matchRef(db('viewer','PAD')), { phase: 'FINISHED' }));
  await assertFails(updateDoc(matchRef(database), { viewerUids: ['outsider'] }));
});
test('event and revision are atomic; forged actor/time, missing details and missing history rejected', async () => {
  const database = await started();
  await assertFails(setDoc(eventRef(database, 'no-history'), event('no-history')));
  await assertFails(writeEvent(database, event('actor', 'actor', 1, { actorUid: 'viewer' })));
  await assertFails(writeEvent(database, event('time', 'time', 1, { recordedAt: new Date(0) })));
  await assertFails(writeEvent(database, event('detail', 'detail', 1, { detail: '' })));
  await assertFails(writeEvent(database, event('bad-side','bad-side',1,{ side: 'UNKNOWN' })));
  await assertFails(writeEvent(database, event('sub','sub',1,{ typeId: 'substitution', player:'p1', otherPlayer:'p1' })));
});
test('unknown player, late minute, correction of side and annulment preserve all history', async () => {
  const database = await started();
  await writeEvent(database, event('goal', 'record', 1, { seconds: 120 }));
  await writeEvent(database, event('goal', 'correct', 2, { seconds: 30, side: 'AWAY' }));
  await writeEvent(database, event('goal', 'void', 3, { seconds: 30, side: 'AWAY', voided: true }));
  const history = await getDocs(collection(database, 'matches/m1/events/goal/history'));
  assert.equal(history.size, 3);
  assert.equal((await getDoc(eventRef(database, 'goal'))).data().voided, true);
  await assertFails(deleteDoc(eventRef(database, 'goal')));
  await assertFails(updateDoc(doc(database,'matches/m1/events/goal/history/record'), { side:'AWAY' }));
  await assertFails(writeEvent(database, event('goal','restore',4)));
});
test('duplicate command acknowledges original even after correction', async () => {
  const database = await started();
  assert.equal(await writeEvent(database, event('g1','op1')), true);
  await writeEvent(database, event('g1','op2',2,{side:'AWAY'}));
  assert.equal(await writeEvent(database, event('g1','op1')), false);
  assert.equal((await getDoc(eventRef(database,'g1'))).data().version,2);
  assert.equal((await getDocs(collection(database,'matches/m1/events/g1/history'))).size,2);
});
test('two operators correcting version one: exactly one wins', async () => {
  const database = await started();
  await writeEvent(database,event('g1'));
  const results = await Promise.allSettled([
    writeEvent(database,event('g1','a',2,{side:'AWAY'})),
    writeEvent(db('other'),event('g1','b',2,{actorUid:'other',seconds:50})),
  ]);
  assert.equal(results.filter(x=>x.status==='fulfilled').length,1);
  assert.equal(results.filter(x=>x.status==='rejected').length,1);
  assert.equal((await getDoc(eventRef(database,'g1'))).data().version,2);
});
test('clock enforces period order, pause/resume, immutable metadata and boundary events', async () => {
  const database = await started();
  await assertFails(clock(database,'FINISHED'));
  await assertFails(updateDoc(matchRef(database),{home:'Alterado'}));
  await clock(database,'PAUSED_FIRST');
  await clock(database,'FIRST');
  await clock(database,'HALF_TIME');
  await clock(database,'SECOND');
  await clock(database,'PAUSED_SECOND');
  await clock(database,'SECOND');
  await clock(database,'FINISHED');
  const m = (await getDoc(matchRef(database))).data();
  assert.equal(m.clockVersion,8);
  assert.equal(m.phase,'FINISHED');
  const events = (await getDocs(collection(database,'matches/m1/events'))).docs.map(x=>x.data());
  assert.equal(events.filter(e=>e.typeId==='period_start').length,2);
  assert.equal(events.filter(e=>e.typeId==='period_end').length,2);
  await assertFails(clock(database,'FIRST'));
  // An omitted event and correction after finalization are still traced.
  await writeEvent(database,event('late'));
  await writeEvent(database,event('late','fix',2,{side:'AWAY'}));
});
test('catalog propagates and disabled or stale new events are rejected', async () => {
  const database = await started();
  await updateDoc(doc(database,'matchCatalogs/cat1'), {
    enabled: types.filter(t=>t!=='offside'), version:2, updatedAt:serverTimestamp(),
  });
  await assertFails(writeEvent(database,event('old')));
  await assertFails(writeEvent(database,event('disabled','disabled',1,{typeId:'offside',catalogVersion:2})));
  await assertSucceeds(writeEvent(database,event('new','new',1,{catalogVersion:2})));
  await assertFails(updateDoc(doc(db('viewer','PAD'),'matchCatalogs/cat1'), { enabled:types,version:3,updatedAt:serverTimestamp() }));
});
test('spectator listener receives goal, correction and void without refresh; measure actual local latency', async () => {
  const database = await started();
  const viewer = db('viewer','PAD');
  let expected;
  let resolveUpdate;
  let rejectUpdate;
  const unsub = onSnapshot(collection(viewer,'matches/m1/events'), snapshot => {
    const e = snapshot.docs.map(x=>x.data()).find(x=>x.eventId==='observed');
    if(e?.version === expected) resolveUpdate?.({ at: performance.now(), event:e });
  }, error=>rejectUpdate?.(error));
  try {
    for(const version of [1,2,3]) {
      expected=version;
      const received=new Promise((resolve,reject)=>{resolveUpdate=resolve;rejectUpdate=reject;});
      const start=performance.now();
      await writeEvent(database,event('observed','op'+version,version,{ side:version>1?'AWAY':'HOME',voided:version===3 }));
      const confirmed=performance.now();
      let timer;
      const result=await Promise.race([received,new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error('listener timeout')),5000);})]).finally(()=>clearTimeout(timer));
      const afterAck=Math.max(0,result.at-confirmed);
      console.log('LOCAL_EMULATOR version='+version+' confirmation_to_listener_ms='+afterAck.toFixed(1)+' request_to_listener_ms='+(result.at-start).toFixed(1));
      assert.ok(afterAck<2000);
      assert.equal(result.event.voided,version===3);
    }
  } finally {unsub();}
});
test('revoked active profile denies operator writes even with signed token', async () => {
  const database = await started();
  await env.withSecurityRulesDisabled(async ctx=>updateDoc(doc(ctx.firestore(),'users/coach'),{isActive:false}));
  await assertFails(writeEvent(database,event('blocked')));
});
