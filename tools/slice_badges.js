const Jimp = require('jimp');
const path = require('path');
const fs = require('fs');

async function slice() {
    const image = await Jimp.read('achivments-jukebox-bg-removed.png');
    const w = image.bitmap.width;
    const h = image.bitmap.height;
    
    const cols = 5;
    const rows = 3;
    
    const cellW = Math.floor(w / cols);
    const cellH = Math.floor(h / rows);
    
    const badgeNames = [
        ["badge_first_scrim.png", "badge_team_creator.png", "badge_scrim_host_10.png", "badge_rated_10.png", "badge_night_owl.png"],
        ["badge_win_streak_5.png", "badge_flawless_victory.png", "badge_five_star.png", "badge_win_streak_10.png", "badge_regional_top.png"],
        ["badge_veteran_100.png", "badge_assassin_master.png", "badge_roamer_master.png", "badge_legend_win.png", "badge_mythic_reached.png"]
    ];
    
    const outDir = 'app/src/main/res/drawable';
    if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
    }
    
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            const name = badgeNames[r][c];
            const x = c * cellW;
            const y = r * cellH;
            
            process.stdout.write(`Slicing ${name} at ${x},${y}\n`);
            
            const badge = image.clone().crop(x, y, cellW, cellH);
            await badge.writeAsync(path.join(outDir, name));
        }
    }
}

slice().catch(console.error);
