# Sweep Game Logic Module

This module contains the core game logic shared between the client and server.

## Classes

- **SweepLogic** - Main game engine
- **Deck** - Card deck management
- **Card** - Card representation
- **Player** - Player state
- **Rank** - Card ranks (ACE, TWO, ..., KING)
- **Suit** - Card suits (HEARTS, DIAMONDS, CLUBS, SPADES)

## Usage

This module is used by:
- `core` - LibGDX client
- `server` - Spring Boot server

Both depend on this module to ensure consistent game rules.
