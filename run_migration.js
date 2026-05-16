const { Client } = require('pg');

const client = new Client({
  connectionString: 'postgresql://postgres:%2BKjkpPVMr639E%2Fn@db.efhbyrhxtsadbqjsfogc.supabase.co:5432/postgres',
  ssl: { rejectUnauthorized: false }
});

async function run() {
  try {
    await client.connect();
    
    const sql = `
      ALTER TABLE profiles
      ADD COLUMN IF NOT EXISTS role TEXT,
      ADD COLUMN IF NOT EXISTS bio TEXT,
      ADD COLUMN IF NOT EXISTS main_heroes TEXT[];
    `;
    
    await client.query(sql);
    console.log('[OK] Database updated successfully with profile columns');
  } catch (err) {
    console.error('[FAIL]', err);
  } finally {
    await client.end();
  }
}

run();
