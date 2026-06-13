# Fast Lane

A multiplayer mobile racing game built with Kotlin, OpenGL ES, and Ktor.

---

## Table of Contents

- [Overview](#overview)
- [Assignment Objectives](#assignment-objectives)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Key Technical Implementations](#key-technical-implementations)
- [Testing](#testing)
- [What I Learned](#what-i-learned)
- [Assets and Libraries](#assets-and-libraries)
- [Demo](#demo)

---

## Overview

Fast Lane is a multiplayer mobile racing game developed as part of a Mobile and Cloud Computing module. Players race through highway traffic, aiming to travel as far as possible without crashing.

The game supports both single-player and multiplayer gameplay, allowing up to four players to compete on the same network.

---

## Assignment Objectives

The primary objective of this assignment was to apply concepts learned throughout the Mobile and Cloud Computing module by implementing three core mobile development features:

| Requirement | Implementation |
|------------|----------------|
| Sensor Controls | Accelerometer-based steering |
| Graphics Rendering | OpenGL ES |
| Networking | Ktor multiplayer networking |

To fulfill these requirements, our team developed Fast Lane, integrating all three features into a complete Android racing game with real-time multiplayer functionality.

---

## Features

- Endless racing gameplay
- Accelerometer-based steering controls
- Real-time multiplayer networking
- OpenGL ES rendering
- Dynamic traffic generation
- Collision detection system
- Infinite road scrolling
- Desktop keyboard controls for testing

---

## Tech Stack

### Languages

- Kotlin

### Mobile Development

- Android SDK
- GLSurfaceView
- Android Sensor Framework

### Graphics

- OpenGL ES 2.0
- GLES20
- GLUtils

### Networking

- Ktor
- Socket-based communication

### Concurrency

- Kotlin Coroutines

---

## Architecture

### Rendering Module

- Renders game objects using OpenGL ES
- Handles textures and sprite rendering
- Maintains graphics performance

### Game Logic Module

- Manages vehicle spawning
- Handles collision detection
- Implements infinite scrolling
- Processes player interactions

### Networking Module

- Supports multiplayer sessions
- Synchronizes player state
- Handles packet communication

### Sensor Module

- Processes accelerometer input
- Converts device tilt into steering controls

---

## Key Technical Implementations

### OpenGL ES Rendering

Implemented a hardware-accelerated rendering pipeline using OpenGL ES and GLSurfaceView to achieve smooth gameplay on mobile devices.

### Accelerometer Controls

Integrated Android's SensorManager to provide intuitive tilt-based vehicle steering.

### Multiplayer Networking

Developed a client-server networking system using Ktor to support real-time multiplayer gameplay.

### Collision Detection

Implemented Axis-Aligned Bounding Box (AABB) collision detection for efficient gameplay interactions.

---

## Testing

The project was tested for:

- Gameplay functionality
- Multiplayer stability
- Accelerometer responsiveness
- Rendering performance
- Collision detection accuracy

---

## What I Learned

Through this project, I gained experience in:

- Android game development
- OpenGL ES graphics programming
- Real-time networking with Ktor
- Mobile sensor integration
- Kotlin coroutines and asynchronous programming
- Software architecture and modular design
- Collision detection techniques
- Collaborative software development within a team environment

---

## Assets and Libraries

### Libraries

- Ktor
- Kotlin Coroutines
- OpenGL ES
- Android Sensor Framework
- AndroidX XR Math Library

### Assets

- Art assets and fonts from Kenney.nl

---

## Demo

![Gameplay Demo](assets/fastlane-demo.gif)

Full gameplay video:
https://youtu.be/your-video-id
