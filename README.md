# Custom Beds & Paintings (Forge 1.20.1)

Turn any picture into a brand-new bed or painting, in game, with no restart.

## How to use
- **Press `B`** to open the **Bed Studio** (rebind in Controls > Custom Beds).
- Drag & drop an image onto the game window, click **Browse**, or paste a file path, then click **Load**.
  PNG, JPG, BMP and GIF work.
- Name it and hit **Create Bed!** The new bed goes straight into your inventory and shows up for everyone on the server right away.
- Every upload makes a **new** bed. Vanilla beds and your older custom beds are never changed.
- The **Bed Library** on the right lists every bed made on the world. You can take copies of your own beds
  (creative players and ops can copy any bed).
- Custom beds work like normal beds: you can sleep in them, set your spawn, and they explode in the Nether.

## Paintings
- Click the **Making: Bed** button at the top of the studio to switch to **Painting**.
- The size (1-4 blocks wide/tall) is picked from your picture's shape; click **Width** / **Height** to change it.
- Hit **Create Painting!** and hang it on any wall with enough room. Break it to get it back.

## Gifts
- Craft **Gift Wrap**: 3 Paper + 1 String + any Dye = 3 Gift Wrap (free in creative).
- Right-click Gift Wrap (or click **Gift Wrap...** in the studio). Pick a bed or painting, pick a friend, add a note.
  - **Wrap It** puts the present in your inventory so you can hand it over yourself.
  - **Wrap & Send** delivers it straight to your friend.
- Right-click a **Wrapped Gift** to unwrap it. A gift addressed to someone can only be opened by them (or the sender).

## Multiplayer
Install the mod on the server **and** every client. Images are stored in the world save, so they come along with backups.

## Building
`gradle build` with Java 17 and Gradle 8.1.1. The finished mod is `build/libs/custombeds-forge-1.20.1-1.0.0.jar`.
Pushing to GitHub runs `.github/workflows/build.yml`, which builds the jar and attaches it to a "latest" release.
