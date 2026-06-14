const WebSocket = require('ws');

const SUPABASE_URL = process.env.SUPABASE_URL || '';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || '';
const SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

function request(method, path, headers, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, SUPABASE_URL);
    const https = require('https');
    const options = {
      hostname: url.hostname, port: 443, path: url.pathname + url.search, method,
      headers: { ...headers, 'Content-Type': 'application/json' }
    };
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(data) }); }
        catch (e) { resolve({ status: res.statusCode, body: data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function main() {
  console.log('=== RAW REALTIME TESTS ===\n');

  // 1. Create user
  const ts = Date.now();
  const testEmail = `test_raw_${ts}@example.com`;
  const testPassword = 'TestPass123!';

  const adminCreate = await request('POST', '/auth/v1/admin/users', {
    'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}`
  }, { email: testEmail, password: testPassword, email_confirm: true });
  const userId = adminCreate.body?.id;
  console.log('User created:', userId);

  const login = await request('POST', '/auth/v1/token?grant_type=password', { 'apikey': SUPABASE_ANON_KEY }, { email: testEmail, password: testPassword });
  const accessToken = login.body?.access_token;
  console.log('Login OK');

  // 2. Create conversation
  const convId = crypto.randomUUID();
  await request('POST', '/rest/v1/conversations', {
    'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}`, 'Prefer': 'return=minimal'
  }, { id: convId, participant_a_id: userId, participant_b_id: userId });
  console.log('Conversation:', convId);

  // 3. Test A: Subscribe with NO filter (all messages inserts)
  console.log('\n--- Test A: Subscribe to ALL messages inserts (no filter) ---');
  const wsA = new WebSocket(`wss://efhbyrhxtsadbqjsfogc.supabase.co/realtime/v1/websocket?apikey=${SUPABASE_ANON_KEY}&vsn=1.0.0`);
  let gotEventA = false;

  await new Promise((resolve) => {
    wsA.on('open', () => {
      wsA.send(JSON.stringify({ topic: 'phoenix', event: 'heartbeat', payload: {}, ref: 1 }));
      setTimeout(() => {
        wsA.send(JSON.stringify({
          topic: 'phoenix', event: 'access_token', payload: { access_token: accessToken }, ref: 2
        }));
      }, 300);
      setTimeout(() => {
        wsA.send(JSON.stringify({
          topic: 'realtime:public:messages',
          event: 'phx_join',
          payload: {
            config: {
              postgres_changes: [{ event: 'INSERT', schema: 'public', table: 'messages' }]
            }
          },
          ref: 3
        }));
      }, 600);
      setTimeout(resolve, 5000);
    });
    wsA.on('message', (data) => {
      const msg = JSON.parse(data);
      if (msg.payload?.postgres_changes) {
        console.log('  🎉 EVENT A:', JSON.stringify(msg.payload.postgres_changes));
        gotEventA = true;
      } else if (msg.event === 'phx_reply') {
        console.log('  Reply:', msg.ref, msg.payload?.status);
      }
    });
  });

  wsA.close();
  console.log('  Test A result:', gotEventA ? 'EVENTS RECEIVED' : 'NO EVENTS');

  // 4. Test B: Subscribe WITH conversation_id filter
  console.log('\n--- Test B: Subscribe to messages with conversation_id filter ---');
  const wsB = new WebSocket(`wss://efhbyrhxtsadbqjsfogc.supabase.co/realtime/v1/websocket?apikey=${SUPABASE_ANON_KEY}&vsn=1.0.0`);
  let gotEventB = false;

  await new Promise((resolve) => {
    wsB.on('open', () => {
      setTimeout(() => {
        wsB.send(JSON.stringify({
          topic: 'phoenix', event: 'access_token', payload: { access_token: accessToken }, ref: 1
        }));
      }, 300);
      setTimeout(() => {
        wsB.send(JSON.stringify({
          topic: 'realtime:public:messages',
          event: 'phx_join',
          payload: {
            config: {
              postgres_changes: [{ event: 'INSERT', schema: 'public', table: 'messages', filter: `conversation_id=eq.${convId}` }]
            }
          },
          ref: 2
        }));
      }, 600);
      // Insert a message while subscribed
      setTimeout(async () => {
        await request('POST', '/rest/v1/messages', {
          'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${accessToken}`, 'Prefer': 'return=minimal'
        }, { id: crypto.randomUUID(), conversation_id: convId, sender_id: userId, content: 'test', client_message_id: `raw-${ts}` });
        console.log('  Message inserted');
      }, 1500);
      setTimeout(resolve, 6000);
    });
    wsB.on('message', (data) => {
      const msg = JSON.parse(data);
      if (msg.payload?.postgres_changes) {
        console.log('  🎉 EVENT B:', JSON.stringify(msg.payload.postgres_changes));
        gotEventB = true;
      } else if (msg.event === 'phx_reply') {
        console.log('  Reply:', msg.ref, msg.payload?.status);
      }
    });
  });

  wsB.close();
  console.log('  Test B result:', gotEventB ? 'EVENTS RECEIVED' : 'NO EVENTS');

  // Cleanup
  await request('DELETE', `/rest/v1/conversations?id=eq.${convId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
  await request('DELETE', `/auth/v1/admin/users/${userId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);

  console.log('\n=== SUMMARY ===');
  console.log('Test A (no filter):', gotEventA ? 'PASS' : 'FAIL');
  console.log('Test B (with filter):', gotEventB ? 'PASS' : 'FAIL');
  console.log(gotEventA || gotEventB ? '\n✅ Realtime is working! Filter issue can be fixed.' : '\n❌ Realtime completely broken. Contact Supabase support.');
}

main().catch(console.error);
