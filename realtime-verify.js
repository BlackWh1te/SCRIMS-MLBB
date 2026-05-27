const WebSocket = require('ws');
const https = require('https');

const SUPABASE_URL = process.env.SUPABASE_URL || '';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || '';
const SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SERVICE_ROLE_KEY) {
  console.error('Missing required env vars: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY');
  process.exit(1);
}

function request(method, path, headers, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, SUPABASE_URL);
    const options = {
      hostname: url.hostname,
      port: 443,
      path: url.pathname + url.search,
      method,
      headers: { ...headers, 'Content-Type': 'application/json' }
    };
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve({ status: res.statusCode, headers: res.headers, body: JSON.parse(data) }); }
        catch (e) { resolve({ status: res.statusCode, headers: res.headers, body: data }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function main() {
  console.log('=== REALTIME WEBSOCKET VERIFICATION ===\n');

  // 1. Create test user and login
  const timestamp = Date.now();
  const testEmail = `test_rt_${timestamp}@example.com`;
  const testPassword = 'TestPass123!';

  console.log('[1] Creating test user...');
  const adminCreate = await request('POST', '/auth/v1/admin/users', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SERVICE_ROLE_KEY}`
  }, { email: testEmail, password: testPassword, email_confirm: true });
  const userId = adminCreate.body?.id;
  console.log('  Status:', adminCreate.status, 'ID:', userId);

  console.log('[2] Logging in...');
  const login = await request('POST', '/auth/v1/token?grant_type=password', { 'apikey': SUPABASE_ANON_KEY }, { email: testEmail, password: testPassword });
  const accessToken = login.body?.access_token;
  console.log('  Status:', login.status);

  // 3. Create a conversation
  console.log('[3] Creating conversation...');
  const convId = crypto.randomUUID();
  const convCreate = await request('POST', '/rest/v1/conversations', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SERVICE_ROLE_KEY}`,
    'Prefer': 'return=representation'
  }, { id: convId, participant_a_id: userId, participant_b_id: userId });
  console.log('  Status:', convCreate.status);

  // 4. Connect WebSocket with AUTHENTICATED token
  console.log('\n[4] Connecting to Realtime WebSocket (authenticated)...');
  const wsUrl = `wss://efhbyrhxtsadbqjsfogc.supabase.co/realtime/v1/websocket?apikey=${SUPABASE_ANON_KEY}&vsn=1.0.0`;
  const ws = new WebSocket(wsUrl);

  let heartbeatInterval;
  let receivedEvents = [];
  let joined = false;

  ws.on('open', () => {
    console.log('  ✅ WebSocket connected');

    heartbeatInterval = setInterval(() => {
      ws.send(JSON.stringify({ topic: 'phoenix', event: 'heartbeat', payload: {}, ref: Date.now() }));
    }, 30000);

    // Authenticate the socket with the user's access token
    setTimeout(() => {
      console.log('[4b] Sending access_token to authenticate socket...');
      ws.send(JSON.stringify({
        topic: 'phoenix',
        event: 'access_token',
        payload: { access_token: accessToken },
        ref: 'auth-1'
      }));
    }, 300);

    // Join the messages channel
    setTimeout(() => {
      console.log('[5] Subscribing to messages channel (authenticated)...');
      ws.send(JSON.stringify({
        topic: 'realtime:public:messages',
        event: 'phx_join',
        payload: {
          config: {
            broadcast: { self: true },
            presence: { key: '' },
            postgres_changes: [
              { event: '*', schema: 'public', table: 'messages', filter: `conversation_id=eq.${convId}` }
            ]
          }
        },
        ref: 'join-1'
      }));
    }, 700);
  });

  ws.on('message', (data) => {
    const msg = JSON.parse(data.toString());
    const event = msg.event;
    const payload = msg.payload;

    // Log all events for debugging
    if (event !== 'heartbeat') {
      receivedEvents.push({ event, ref: msg.ref, payloadType: payload ? Object.keys(payload) : 'none' });
    }

    if (event === 'phx_reply' && msg.ref === 'join-1') {
      if (payload?.status === 'ok') {
        joined = true;
        console.log('  ✅ Channel joined successfully');
        console.log('     Response keys:', Object.keys(payload));

        // Insert message after successful join
        setTimeout(async () => {
          console.log('[6] Inserting message via AUTHENTICATED user to trigger realtime...');
          const msgInsert = await request('POST', '/rest/v1/messages', {
            'apikey': SUPABASE_ANON_KEY,
            'Authorization': `Bearer ${accessToken}`,
            'Prefer': 'return=representation'
          }, {
            conversation_id: convId,
            sender_id: userId,
            sender_name: 'TestRT',
            content: 'Realtime test message ' + timestamp,
            type: 'text',
            client_message_id: `cm_rt_${timestamp}`,
            delivery_status: 'SENT'
          });
          console.log('  Insert status:', msgInsert.status);
          if (msgInsert.body?.length > 0) {
            console.log('  Message ID:', msgInsert.body[0].id);
          }
        }, 1000);
      } else {
        console.log('  ❌ Channel join failed:', payload);
      }
    }

    if (event === 'postgres_changes' || event === 'INSERT' || event === 'UPDATE') {
      console.log('  ✅ Realtime event:', event, payload);
    }
  });

  ws.on('error', (err) => {
    console.log('  ❌ WebSocket error:', err.message);
  });

  ws.on('close', (code, reason) => {
    console.log(`  WebSocket closed (code=${code})`);
    clearInterval(heartbeatInterval);
  });

  // Wait for events
  await new Promise(resolve => setTimeout(resolve, 8000));

  console.log('\n[7] Results:');
  console.log('  Events received:', receivedEvents.length);
  receivedEvents.forEach((e, i) => {
    console.log(`    ${i + 1}. event=${e.event} ref=${e.ref} keys=${e.payloadType}`);
  });

  if (joined && receivedEvents.some(e => e.event === 'postgres_changes' || e.event === 'INSERT')) {
    console.log('  ✅ Realtime subscription WORKING for authenticated user!');
  } else if (joined) {
    console.log('  ⚠️ Channel joined but no postgres_changes events. Check:');
    console.log('     1. Is REPLICA IDENTITY set on messages table?');
    console.log('     2. Is the messages table in supabase_realtime publication?');
    console.log('     3. Does the authenticated user have RLS SELECT on messages?');
  } else {
    console.log('  ❌ Channel join failed');
  }

  // Cleanup
  console.log('\n[8] Cleanup...');
  ws.close();
  await request('DELETE', `/rest/v1/conversations?id=eq.${convId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
  await request('DELETE', `/auth/v1/admin/users/${userId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
  console.log('  Done');
}

main().catch(console.error);
