const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  connectionString: 'postgresql://postgres:%2BKjkpPVMr639E%2Fn@db.efhbyrhxtsadbqjsfogc.supabase.co:5432/postgres',
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
