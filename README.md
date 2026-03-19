# WizardHistory – Addon for WizardCommand

An addon that automatically tracks the command history of `/unioncommand` per player,
with PlaceholderAPI support to display a **Gacha History** system in the style of Genshin Impact.

---

## Features

| Feature | Description |
|---|---|
| **Auto-track** | Automatically logs every time `/unioncommand <name>` is executed |
| **Per-player history** | Each player has their own history file (`data/<UUID>.yml`) |
| **Name masking** | `display-names` in config renames entries → players don't see the real command name |
| **Pity counter** | `%wizhistory_pity_<cmd>%` counts pulls since the last occurrence of that result |
| **Pagination** | Placeholders support pagination for use in scoreboards/GUIs |
| **Whitelist/Blacklist** | Filter which commands to track and which to ignore |

---

## Requirements

- **Spigot / Paper 1.21+**
- **WizardCommand** (main plugin)
- **PlaceholderAPI** (for placeholders)
- Java 21

---

## Build

```bash
# Clone / extract the project into the WizardHistory/ folder
cd WizardHistory/
mvn clean package
# The JAR will be located at target/WizardHistory-1.0.0.jar
```

Copy `target/WizardHistory-1.0.0.jar` into your server's `plugins/` folder.

---

## Folder Structure

```
plugins/
  WizardHistory/
    config.yml          ← main configuration
    data/
      <UUID>.yml        ← history file for each player
```

---

## config.yml – Explanation

```yaml
max-history: 200          # Maximum number of entries per player

date-format: "yyyy/MM/dd HH:mm"   # Timestamp format

# DISPLAY NAME OVERRIDES – important for masking real command names!
# Key = WizardCommand file name (without .yml)
display-names:
  gacha_standard:   "Standard Wish"
  gacha_character:  "Character Event Wish"
  gacha_weapon:     "Weapon Event Wish"

# Only track these commands (leave empty to track all)
track-only:
  - gacha_standard
  - gacha_character
  - gacha_weapon

# Do not track these commands
ignore-commands:
  - reload
```

---

## Full Placeholder Reference

### Summary

| Placeholder | Description |
|---|---|
| `%wizhistory_total%` | Total number of pulls for the player |
| `%wizhistory_total_<cmd>%` | Total pulls for a specific command |

### Entries by index (1 = most recent)

| Placeholder | Description |
|---|---|
| `%wizhistory_entry_1_name%` | Display name of the most recent pull |
| `%wizhistory_entry_1_raw%` | Real command name of the most recent pull |
| `%wizhistory_entry_1_time%` | Timestamp of the most recent pull |
| `%wizhistory_entry_5_name%` | Display name of the 5th most recent pull |

### Shortcuts (most recent pull)

| Placeholder | Description |
|---|---|
| `%wizhistory_last_name%` | Display name of the most recent pull |
| `%wizhistory_last_raw%` | Real command name of the most recent pull |
| `%wizhistory_last_time%` | Timestamp of the most recent pull |

### Pity counter (Genshin-style)

```
%wizhistory_pity_<command_name>%
```
Counts the number of pulls **since the last time** that command appeared in history.

**Example – 5-star pity:**
If you have a command `gacha_5star` that runs when a player gets a 5-star result:
```
Pulls since last 5-star: %wizhistory_pity_gacha_5star%
```

### Pagination (for GUIs / books)

```
%wizhistory_page_<page>_<per_page>_<slot>_name%
%wizhistory_page_<page>_<per_page>_<slot>_time%
%wizhistory_page_<page>_<per_page>_<slot>_raw%
%wizhistory_maxpage_<per_page>%
```

**Example** – Scoreboard showing the 5 most recent pulls:
```
%wizhistory_page_1_5_1_name%  | %wizhistory_page_1_5_1_time%
%wizhistory_page_1_5_2_name%  | %wizhistory_page_1_5_2_time%
%wizhistory_page_1_5_3_name%  | %wizhistory_page_1_5_3_time%
%wizhistory_page_1_5_4_name%  | %wizhistory_page_1_5_4_time%
%wizhistory_page_1_5_5_name%  | %wizhistory_page_1_5_5_time%
```

---

## Admin Commands

```
/wizhistory reload                    Reload config (no server restart needed)
/wizhistory clear <player>            Clear all history for a player
/wizhistory view <player> [page]      View a player's history in chat
```

**Permission:** `wizhistory.admin` (default: op)

---

## How It Works (Technical)

1. WizardCommand uses the command `/unioncommand <file_name> [player]`
2. WizardHistory listens to `PlayerCommandPreprocessEvent` and `ServerCommandEvent`
3. When `/unioncommand` is detected, it extracts the file name and writes it to that player's history
4. Data is saved to `plugins/WizardHistory/data/<UUID>.yml`
5. PlaceholderAPI reads the data and returns the appropriate placeholder values when requested

---

## Gacha History Example (Genshin Impact Style)

Use **DeluxeMenus** or **TrMenu** to create a GUI:

```yaml
# Example item in DeluxeMenus
display_name: '&f%wizhistory_entry_1_name%'
lore:
  - '&7%wizhistory_entry_1_time%'
  - ''
  - '&7Total pulls: &f%wizhistory_total%'
  - '&7Pity 5★: &f%wizhistory_pity_gacha_5star%'
```

---

## data/<UUID>.yml Structure

```yaml
history:
  - command: gacha_standard
    timestamp: 1727100000000
  - command: gacha_character
    timestamp: 1727099000000
```