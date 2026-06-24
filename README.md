# Live TV with Channel Reordering (Android TV)

This is a customized fork of the open-source Android TV **LiveTV (Live Channels)** application from AOSP.

---

## Key Features: Manual Channel Reordering
Unlike the standard AOSP app which relies strictly on the system's `TvProvider` database (which requires system/firmware signatures to modify), this fork:
* Stores a custom mapping of `channelId` to a user-defined display number locally on the device (`SharedPreferences`).
* Provides a visual **"Reorder channels"** menu inside the settings panel.
* Allows you to select any channel with the remote control (**OK** key), move it up or down using the **Dpad Up/Down** keys, and press **OK/Back** to save.
* Works seamlessly with the EPG grid, side panel, and channel zapping (pressing channel numbers on the remote).

---

*Built with AI aid. Distributed under the original Apache 2.0 license.*