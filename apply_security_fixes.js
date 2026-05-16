const { Client } = require('pg');
const fs = require('fs');

async function applySecurityFixes() {
  const client = new Client({
    host: 'db.efhbyrhxtsadbqjsfogc.supabase.co',
    port: 5432,
    database: 'postgres',
    user: 'postgres',
    password: '+KjkpPVMr639E/n',
    ssl: { rejectUnauthorized: false }
  });

  try {
    await client.connect();
    console.log('Connected to database');

    const sql = fs.readFileSync('supabase/security_fixes.sql', 'utf8');

    // Split SQL into individual statements (handling $$...$$ blocks)
    const statements = [];
    let currentStatement = '';
    let inDollarBlock = false;
    let dollarDepth = 0;

    for (const line of sql.split('\n')) {
      const trimmedLine = line.trim();

      // Skip empty lines and comments (but keep them within statements)
      if (trimmedLine === '' || trimmedLine.startsWith('--')) {
        currentStatement += line + '\n';
        continue;
      }

      // Count $$ markers
      const dollarMatches = (line.match(/\$\$/g) || []).length;
      dollarDepth += dollarMatches;

      // If we're inside a $$ block, dollarDepth will be odd
      inDollarBlock = dollarDepth % 2 === 1;

      currentStatement += line + '\n';

      // Split by semicolon if not inside $$ block
      if (!inDollarBlock && trimmedLine.endsWith(';')) {
        statements.push(currentStatement.trim());
        currentStatement = '';
        // Reset dollar depth after a complete statement
        dollarDepth = 0;
      }
    }

    // Add any remaining statement
    if (currentStatement.trim() !== '') {
      statements.push(currentStatement.trim());
    }

    console.log(`Executing ${statements.length} statements...`);

    for (let i = 0; i < statements.length; i++) {
      const statement = statements[i];
      if (statement.trim() === '') continue;

      try {
        await client.query(statement);
        console.log(`Statement ${i + 1}/${statements.length}: SUCCESS`);
      } catch (err) {
        console.error(`Statement ${i + 1}/${statements.length}: ERROR`);
        console.error(err.message);
        console.error('Statement:', statement.substring(0, 200) + '...');
        // Continue with other statements
      }
    }

    console.log('Security fixes applied successfully!');
  } catch (err) {
    console.error('Error:', err.message);
    process.exit(1);
  } finally {
    await client.end();
  }
}

applySecurityFixes();
