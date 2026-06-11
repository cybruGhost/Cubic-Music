# 
<p align="center">
  <img width="1024" height="1024" alt="Image" src="https://github.com/user-attachments/assets/15bd0440-9b2d-4de9-b823-ecd8434889f9" />
</p>

<h1 align="center" style="font-size: 3.2rem;">Cubic Music</h1>

<div align="center">

[![Latest Version](https://img.shields.io/github/v/release/cybruGhost/Cubic-Music?label=Latest%20Version)](https://github.com/cybruGhost/Cubic-Music/releases/latest)  
[![Total Downloads](https://img.shields.io/github/downloads/cybruGhost/Cubic-Music/total?label=Total%20Downloads)](https://github.com/cybruGhost/Cubic-Music/releases)  
[![Latest Release Downloads](https://img.shields.io/github/downloads/cybruGhost/Cubic-Music/latest/total?label=Latest%20Release%20Downloads)](https://github.com/cybruGhost/Cubic-Music/releases/latest)  
[![License: GPL v3](https://img.shields.io/github/license/cybruGhost/Cubic-Music?color=blue)](https://www.gnu.org/licenses/gpl-3.0)

[![Crowdin](https://img.shields.io/badge/𝐓𝐑𝐀𝐍𝐒𝐋𝐀𝐓𝐄-2E3340?style=for-the-badge&logo=crowdin&logoColor=white)](https://crowdin.com/project/cubic-music)

<br/>

**Cubic Music** is a powerful, multilingual YouTube Music frontend for Android.  
It features a sleek, modern UI focused on performance and user freedom, offering seamless streaming, full downloads, and stunning visuals.

*While not as polished as some mature apps yet, it remains a solid, lightweight, and visually appealing alternative — actively maintained with frequent improvements.*

🚀 **[Download Latest Release](https://github.com/cybruGhost/Cubic-Music/releases/latest)**  
⭐ **Star this repository to support development!**

</div>

---

## 🚀 Current Version: `v1.8.2` (Beta)

🔧 **Status:** Active development — continuous improvements and new features.

---

## 🆕 What’s New in v1.8.2

### 🖼️ Spotify Canvas (Beta)

Short looping visuals (3–8s) replace static album covers during playback, enhancing the mood and identity of each track.

<p align="center">
  <img src="https://github.com/user-attachments/assets/00de30f0-b436-4a3d-8b59-5af9a0a44881" width="280" alt="Cubic Music Screenshot 1"/>
  <img src="https://github.com/user-attachments/assets/a40120a1-73b1-455a-860d-68e75175d2c2" width="280" alt="Cubic Music Screenshot 2"/>
  <img src="https://github.com/user-attachments/assets/830f4a15-ac76-422f-9762-a69e17b34568" width="280" alt="Cubic Music Screenshot 3"/>
</p>

---

## ✨ Key Features

### 🔁 Music Rewind
Cubic Music’s take on *Spotify Wrapped* — a smart, shareable recap of your listening habits:

- 🎧 Top songs & artists  
- ⏱️ Total listening time  
- 🔥 Favorite genres & moods  
- 📅 Listening streaks & highlights  

---

### 🆕 Latest Additions
- **Spotify Playlist Import**  
- **Explicit Content Tags**  
- **Comments System**  

---

### 🎧 Core Capabilities
- **Smart Caching** (custom size)  
- **Batch Downloads** (songs & playlists)  
- **Full Offline Mode**  
- **Background Playback**  
- **Advanced Audio Controls:** speed, pitch, normalization, skip silence  
- **Audio Visualizers** (multiple styles)  

---

### 🎨 Customization & UI
- **Dynamic Themes**  
- **Lyrics Engine:** fetch, edit, translate, synced/unsynced  
- **Playlist Management:** import/export, RiMusic-compatible  
- **Custom Sleep Timer**  

---

### 📱 Integrations & Utilities
- **Android Auto & Android TV**  
- **Homescreen Widgets** *(experimental)*  
- **In-app Update Checker**  
- **Full Data Export:** settings, downloads, cache  

---

## 🛠️ Roadmap & To‑Do

- [ ] Improved playlist mood detection  
- [ ] Memory optimization for large caches  
- [ ] UI refinements inspired by RiMusic  
- [ ] General performance improvements  
- [ ] More languages  

---

# 📋 Supported Playlist Formats

Cubic Music supports multiple CSV playlist formats.

---

## 🧩 1. Compatible App Format (Native)

Imported directly — no conversion needed.  
Uses raw YouTube video IDs.

```csv
PlaylistBrowseId,PlaylistName,MediaId,Title,Artists,Duration,ThumbnailUrl
,Swipefy,1pEe7-tWv2M,Good Grief,Jenna Raine,160,https://inv.perditum.com/vi/1pEe7-tWv2M/hqdefault.jpg
```

---

## 🧱 2. Extended App Format (With Album Info)

```csv
PlaylistBrowseId,PlaylistName,MediaId,Title,Artists,Duration,ThumbnailUrl,AlbumId,AlbumTitle,ArtistIds
,MyPlaylist,abc123,Song Title,Artist Name,180,https://example.com/thumb.jpg,album123,Album Name,artist123
```

---

## 🎧 3. Spotify Export Format

```csv
Track Name,Artist Name(s),Track Duration (ms),Album Name,Album Image URL,Explicit
Blinding Lights,The Weeknd,200040,After Hours,https://i.scdn.co/image/ab67616d0000b273,false
```

---

## 💽 4. Exportify Format (Enhanced Spotify Export)

```csv
Track URI,Track Name,Artist Name(s),Album Name,Album Image URL,Track Duration (ms),Explicit,Playlist Name
spotify:track:123,Blinding Lights,The Weeknd,After Hours,https://i.scdn.co/image/ab67616d0000b273,200040,false,My Playlist
```

---

## 🪶 5. Custom Minimal Format

```csv
PlaylistBrowseId,PlaylistName,MediaId,Title,Artists,Duration
,MyPlaylist,abc123,Song Title,Artist Name,180
```

---

## 🔄 Conversion Rules

### ✅ Direct Import (no conversion)
- App Format  
- Extended App Format  
- Custom Format  

### 🔁 Conversion Required (YouTube API)
- Spotify Export  
- Exportify  

### 📦 Standardized Output Format

```csv
PlaylistBrowseId,PlaylistName,MediaId,Title,Artists,Duration,ThumbnailUrl
,Imported Playlist,1pEe7-tWv2M,Good Grief,Jenna Raine,160,https://yt.omada.cafe/vi/1pEe7-tWv2M/hqdefault.jpg
```

---

# 📊 Live System Status & Stats

## 📥 Download Counters

| Metric | Badge | Description |
|-------|--------|-------------|
| **All-Time Downloads** | ![Total Downloads](https://img.shields.io/github/downloads/cybruGhost/Cubic-Music/total?label=Total%20Downloads&color=blue) | Across all versions |
| **Current Release** | ![Latest Release Downloads](https://img.shields.io/github/downloads/cybruGhost/Cubic-Music/latest/total?label=Latest%20Release&color=green) | `v1.8.2` only |
| **Version-Specific** | ![v1.8.0 Downloads](https://img.shields.io/github/downloads/cybruGhost/Cubic-Music/v1.8.0/total?label=v1.8.0%20Downloads&color=orange) | Specific build |

---

## ⚙️ API Status

![API Status](https://img.shields.io/badge/dynamic/json?label=API&query=$.0.type&url=https://yt.omada.cafe/api/v1/search?q=test&color=brightgreen)
![Search](https://img.shields.io/badge/dynamic/json?label=Search&query=$.0.type&prefix=OK&url=https://yt.omada.cafe/api/v1/search?q=test)
![Results](https://img.shields.io/badge/dynamic/json?label=Results&query=$.length&url=https://yt.omada.cafe/api/v1/search?q=test)

---

# 🌍 Help Translate Cubic Music

Join our Crowdin project to help bring Cubic Music to more languages:

[![Crowdin](https://img.shields.io/badge/𝐓𝐑𝐀𝐍𝐒𝐋𝐀𝐓𝐄-2E3340?style=for-the-badge&logo=crowdin&logoColor=white)](https://crowdin.com/project/cubic-music)

---

# ❤️ Support the Project

If you enjoy Cubic Music, consider supporting development:

☕ **[Support via Fourthwall Shop](https://cyberghost-shop.fourthwall.com/)**

---

# ⚖️ Legal Notice & Disclaimer

- **Copyright:** Cubic Music respects copyrights and **does not enable illegal downloads**.  
- **Affiliation:** Not affiliated with YouTube, Google LLC, or their partners.  
All trademarks belong to their respective owners.

---

<div align="center">

Made with ❤️ by [cybruGhost](https://github.com/cybruGhost)  
⭐ **Don’t forget to star and follow for updates!** 🚀

</div>

