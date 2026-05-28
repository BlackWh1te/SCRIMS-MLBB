const { Client } = require('pg');
const fs = require('fs');

const connectionString = process.env.DATABASE_URL;
if (!connectionString) {
  console.error('DATABASE_URL environment variable is required');
  process.exit(1);
}
const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false }
});

async function run() {
  try {
    const sql = fs.readFileSync('supabase/migrations/20260531060002_backfill_lfg_avatars.sql', 'utf8');
    await client.connect();
    await client.query(sql);
    console.log('[OK] Backfill LFG avatars successfully');
  } catch (err) {
    console.error('[FAIL]', err);
  } finally {
    await client.end();
  }
}

run();
