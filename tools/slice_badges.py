from PIL import Image
import os

def slice_image(input_path, output_dir):
    img = Image.open(input_path)
    img_w, img_h = img.size
    
    # 5 columns, 3 rows
    cols = 5
    rows = 3
    
    cell_w = img_w // cols
    cell_h = img_h // rows
    
    badge_names = [
        ["badge_first_scrim.png", "badge_team_creator.png", "badge_scrim_host_10.png", "badge_rated_10.png", "badge_night_owl.png"],
        ["badge_win_streak_5.png", "badge_flawless_victory.png", "badge_five_star.png", "badge_win_streak_10.png", "badge_regional_top.png"],
        ["badge_veteran_100.png", "badge_assassin_master.png", "badge_roamer_master.png", "badge_legend_win.png", "badge_mythic_reached.png"]
    ]
    
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    for r in range(rows):
        for c in range(cols):
            left = c * cell_w
            top = r * cell_h
            right = left + cell_w
            bottom = top + cell_h
            
            # Crop the cell
            cell = img.crop((left, top, right, bottom))
            
            # Save to output
            name = badge_names[r][c]
            output_path = os.path.join(output_dir, name)
            cell.save(output_path)
            print(f"Saved {name}")

if __name__ == "__main__":
    slice_image('achivments-jukebox-bg-removed.png', 'app/src/main/res/drawable')
