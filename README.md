# AimTrainer
README  ·  May 2025
1. Project Overview
Aim Trainer is a timed target-clicking game built entirely with Java Swing. The player clicks on randomly spawned geometric shapes before they disappear, racing against a 30-second countdown. The game tracks score, hit rate, accuracy, and personal best scores — all saved to local text files so progress persists between sessions.
2. What the Project Does
Randomly spawns three target shapes — circles (60%), diamonds (25%), and triangles (15%) — at random positions with random sizes and lifetimes.
Targets fade out and disappear if not clicked in time. Hitting a target early awards more points than hitting it near expiry.
Four difficulty levels (Easy, Medium, Hard, Insane) control target size, how long targets stay on screen, and how frequently they spawn.
A HUD bar displays live score, time remaining, accuracy percentage, and the player's personal best score during gameplay.
Visual hit effects play on every successful click — a floating '+pts' text label and a burst explosion animation.
A login and register system stores credentials in users.txt and personal best scores in scores.txt.
A result screen at the end of each round shows full stats and highlights new personal best scores in gold.

3. How to Run the Program
Requirements

A terminal / command prompt
No external libraries required — uses only standard Java SE

Steps
Download or clone the repository.
Open a terminal and navigate to the folder containing all .java files.
Compile all files at once:
javac *.java
Run the application:
java AimTrainer
Register an account on the login screen, then log in to play.

4. Project Goals and Purpose
This project was built to demonstrate core Java and object-oriented programming concepts through an interactive game:
Abstract classes and inheritance — Target is abstract; CircleTarget, DiamondTarget, and TriangleTarget extend it. HitEffect is abstract; FloatingTextEffect and ExplosionEffect extend it.
Encapsulation — GameState holds all game data with no rendering logic; UserManager handles all file I/O with no UI code.
Polymorphism — GamePanel holds List<Target> and List<HitEffect> and calls draw() on each without knowing the concrete type.
Event-driven programming — MouseListener handles clicks; a javax.swing.Timer drives the 60fps game loop.
File I/O — UserManager reads and writes users.txt and scores.txt for persistent login and high score tracking.
GUI design — LoginPanel and GamePanel are styled with a consistent dark theme, animations, and real-time feedback.

5. Installation and Setup
No installation or external setup is required beyond a standard JDK. The program creates its own data files automatically:
users.txt — created when the first account is registered. Format: username:password
scores.txt — created when the first round is completed. Format: username:highscore
Both files are created in whatever directory you run the java command from. If you move the executable, move these files with it to keep your data.
