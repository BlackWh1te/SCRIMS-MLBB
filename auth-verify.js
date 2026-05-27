const https = require('https');
const crypto = require('crypto');

const SUPABASE_URL = 'https://efhbyrhxtsadbqjsfogc.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVmaGJ5cmh4dHNhZGJxanNmb2djIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg2MTkzMjQsImV4cCI6MjA5NDE5NTMyNH0.6Ywj8Xxg0mkKnp6umnWG7a6jTqCdH7tJ3EacZpkGl0E';
const SERVICE_ROLE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVmaGJ5cmh4dHNhZGJxanNmb2djIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3ODYxOTMyNCwiZXhwIjoyMDk0MTk1MzI0fQ.PO8zk6lfZjSMdz3QzjJaJm8xLuRK-utQbQz3dHqAl_Q';

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
  const timestamp = Date.now();
  const testEmail = `test_auth_${timestamp}@example.com`;
  const testPassword = 'TestPass123!';

  console.log('=== AUTHENTICATED MESSAGING VERIFICATION ===\n');

  // 1. Create user via admin API (service role)
  console.log('[1] Creating test user via admin API:', testEmail);
  const adminCreate = await request('POST', '/auth/v1/admin/users', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SERVICE_ROLE_KEY}`
  }, {
    email: testEmail,
    password: testPassword,
    email_confirm: true
  });
  console.log('  Status:', adminCreate.status);
  
  let userId, accessToken;
  
  if (adminCreate.status === 201 || adminCreate.status === 200) {
    userId = adminCreate.body?.id;
    console.log('  User created. ID:', userId);
  } else {
    console.log('  Body:', JSON.stringify(adminCreate.body, null, 2));
    console.log('\n  Admin create failed. Trying to use existing test user...');
    return;
  }

  // 2. Login as the new user
  console.log('\n[2] Logging in as test user...');
  const login = await request('POST', '/auth/v1/token?grant_type=password', { 'apikey': SUPABASE_ANON_KEY }, { email: testEmail, password: testPassword });
  console.log('  Status:', login.status);
  if (login.status !== 200) {
    console.log('  Login failed:', login.body);
    return;
  }
  accessToken = login.body?.access_token;
  userId = login.body?.user?.id;
  console.log('  JWT obtained. User ID:', userId);

  // 3. Authenticated GET conversations (empty expected for new user)
  console.log('\n[3] GET /rest/v1/conversations (authenticated, new user)...');
  const convGet = await request('GET', `/rest/v1/conversations?select=*&limit=5`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${accessToken}` }, null);
  console.log('  Status:', convGet.status, 'Count:', convGet.body?.length || 0);

  // 4. Create a second test user for participant_b, and a scrim
  console.log('\n[4a] Creating participant_b user...');
  const bEmail = `test_b_${timestamp}@example.com`;
  const bUser = await request('POST', '/auth/v1/admin/users', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SERVICE_ROLE_KEY}`
  }, { email: bEmail, password: testPassword, email_confirm: true });
  const bUserId = bUser.body?.id;
  console.log('  Status:', bUser.status, 'ID:', bUserId);

  console.log('[4b] Creating test conversation (user is participant_a, no scrim)...');
  const newConvId = crypto.randomUUID();
  const convCreate = await request('POST', '/rest/v1/conversations', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${SERVICE_ROLE_KEY}`,
    'Prefer': 'return=representation'
  }, {
    id: newConvId,
    participant_a_id: userId,
    participant_b_id: bUserId || userId
  });
  console.log('  Status:', convCreate.status);
  if (convCreate.status >= 400) {
    console.log('  Body:', JSON.stringify(convCreate.body, null, 2));
  }

  // 5. User reads their conversation
  console.log('\n[5] User GET /conversations (expect to see the new one)...');
  const convUserGet = await request('GET', `/rest/v1/conversations?id=eq.${newConvId}&select=*`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${accessToken}` }, null);
  console.log('  Status:', convUserGet.status, 'Rows:', convUserGet.body?.length || 0);
  if (convUserGet.body?.length > 0) {
    console.log('  ✅ Conversation accessible to user!');
  } else {
    console.log('  ❌ Conversation NOT visible to user (RLS blocking)');
  }

  // 6. User posts a message
  console.log('\n[6] User POST /messages with client_message_id...');
  const msgInsert = await request('POST', '/rest/v1/messages', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${accessToken}`,
    'Prefer': 'return=representation'
  }, {
    conversation_id: newConvId,
    sender_id: userId,
    sender_name: 'TestUser',
    content: 'Hello authenticated world!',
    type: 'text',
    client_message_id: `cm_auth_${timestamp}`,
    delivery_status: 'SENT'
  });
  console.log('  Status:', msgInsert.status);
  if (msgInsert.status === 201 && msgInsert.body?.length > 0) {
    console.log('  ✅ Message inserted! ID:', msgInsert.body[0].id);
  } else {
    console.log('  Body:', JSON.stringify(msgInsert.body, null, 2));
  }

  // 7. User reads messages
  console.log('\n[7] User GET /messages for conversation...');
  const msgGet = await request('GET', `/rest/v1/messages?conversation_id=eq.${newConvId}&select=*`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${accessToken}` }, null);
  console.log('  Status:', msgGet.status, 'Count:', msgGet.body?.length || 0);

  // 8. Test idempotency
  console.log('\n[8] Testing idempotency (duplicate client_message_id)...');
  const dupInsert = await request('POST', '/rest/v1/messages', {
    'apikey': SUPABASE_ANON_KEY,
    'Authorization': `Bearer ${accessToken}`,
    'Prefer': 'return=representation'
  }, {
    conversation_id: newConvId,
    sender_id: userId,
    sender_name: 'TestUser',
    content: 'Hello authenticated world!',
    type: 'text',
    client_message_id: `cm_auth_${timestamp}`,
    delivery_status: 'SENT'
  });
  console.log('  Status:', dupInsert.status);
  if (dupInsert.status === 409) {
    console.log('  ✅ Duplicate rejected as expected!');
  } else if (dupInsert.status === 400 && dupInsert.body?.code === '23505') {
    console.log('  ✅ Duplicate rejected (23505 unique constraint)!');
  } else {
    console.log('  Body:', JSON.stringify(dupInsert.body, null, 2));
  }

  // 9. Cleanup
  console.log('\n[9] Cleanup - delete test data...');
  if (newConvId) {
    const delConv = await request('DELETE', `/rest/v1/conversations?id=eq.${newConvId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
    console.log('  Delete conv status:', delConv.status);
  }

  if (bUserId) {
    const delB = await request('DELETE', `/auth/v1/admin/users/${bUserId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
    console.log('  Delete user B status:', delB.status);
  }
  if (userId) {
    const delUser = await request('DELETE', `/auth/v1/admin/users/${userId}`, { 'apikey': SUPABASE_ANON_KEY, 'Authorization': `Bearer ${SERVICE_ROLE_KEY}` }, null);
    console.log('  Delete user A status:', delUser.status);
  }

  console.log('\n=== VERIFICATION COMPLETE ===');
}

main().catch(console.error);
