const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

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
    await client.connect();
    
    const sqlPath = path.join(__dirname, 'lfg_migration.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');
    
    await client.query(sql);
    console.log('[OK] LFG Database migration completed successfully');
  } catch (err) {
    console.error('[FAIL]', err);
  } finally {
    await client.end();
  }
}

run();
