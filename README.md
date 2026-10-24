# 🎮 SteamCraft - Multijugador P2P de Minecraft vía Steam
**SteamCraft** es un mod para Minecraft que permite jugar multijugador con tus amigos utilizando la red Peer-to-Peer (P2P) de **Steam** mediante el **AppID 480 (Spacewar)**. 

### 🚀 Ventajas:
* **Sin abrir puertos ni configurar routers** (cero Port Forwarding).
* **Sin programas externos** como Hamachi, Radmin VPN o ngrok.
* **Túnel directo y seguro** mediante los relays y sockets de Steam Datagram Relay (SDR).
* **Integración total con la lista de amigos y el overlay de Steam (Shift + Tab)**.

---

## 📦 Versiones Listas para Descargar y Compartir

Los archivos están organizados por plataforma y versión en sus respectivas carpetas:

```
SteamCraft-Multiplayer/
├── Fabric/ (Mods para Cliente / LAN)
│   ├── steamcraft-1.16.5.jar
│   ├── steamcraft-1.18.2.jar
│   ├── steamcraft-1.19.2.jar
│   ├── steamcraft-1.19.4.jar
│   ├── steamcraft-1.20.1.jar
│   ├── steamcraft-1.20.4.jar
│   └── steamcraft-1.21.1.jar
├── Forge/ (Mods para Cliente / LAN)
│   ├── steamcraft-1.7.10.jar
│   ├── steamcraft-1.12.2.jar
│   ├── steamcraft-1.16.5.jar
│   ├── steamcraft-1.18.2.jar
│   ├── steamcraft-1.19.2.jar
│   ├── steamcraft-1.19.4.jar
│   ├── steamcraft-1.20.1.jar
│   └── steamcraft-1.20.4.jar
├── NeoForge/ (Mods para Cliente / LAN)
│   ├── steamcraft-1.20.4.jar
│   └── steamcraft-1.21.1.jar
└── Servidor/ (Para Servidores Dedicados 24/7)
    ├── Paper-Spigot/
    │   └── steamcraft-spigot.jar (Plugin universal: 1.12.2 a 1.21.1+)
    ├── Fabric/
    │   ├── steamcraft-server-1.16.5.jar
    │   ├── steamcraft-server-1.18.2.jar
    │   ├── steamcraft-server-1.19.2.jar
    │   ├── steamcraft-server-1.19.4.jar
    │   ├── steamcraft-server-1.20.1.jar
    │   ├── steamcraft-server-1.20.4.jar
    │   └── steamcraft-server-1.21.1.jar
    ├── Forge/
    │   ├── steamcraft-server-1.7.10.jar
    │   ├── steamcraft-server-1.12.2.jar
    │   ├── steamcraft-server-1.16.5.jar
    │   ├── steamcraft-server-1.18.2.jar
    │   ├── steamcraft-server-1.19.2.jar
    │   ├── steamcraft-server-1.19.4.jar
    │   ├── steamcraft-server-1.20.1.jar
    │   └── steamcraft-server-1.20.4.jar
    └── NeoForge/
        ├── steamcraft-server-1.20.4.jar
        └── steamcraft-server-1.21.1.jar
```

> **Universal (Cliente LAN + Servidor Dedicado):** Todos los archivos de Fabric, Forge y NeoForge son híbridos universales. Si los colocas en la carpeta `mods` de un servidor dedicado (`server.jar`), el mod detectará automáticamente el servidor dedicado y publicará el puerto en Steam sin requerir interfaz gráfica.
> **Servidores Paper / Spigot:** Si tienes un servidor dedicado Paper o Spigot, simplemente coloca `steamcraft-spigot.jar` en la carpeta `plugins/` de tu servidor. Compatible con versiones 1.12.2 hasta 1.21.1+.


## ⚙ Requisitos Previos

1. **Steam abierto en segundo plano:**
   - Tanto la persona que crea el mundo (Host) como quienes se unan deben tener el cliente de Steam abierto y haber iniciado sesión en su cuenta.
   - Ambas personas deben tenerse agregadas mutuamente como amigos en Steam.
2. **Librerías nativas:**
   - Todos los archivos `.jar` de SteamCraft ya vienen con las librerías nativas empaquetadas (`steam_api64.dll`, `steamworks4j64.dll`, Linux `.so` y macOS `.dylib`), por lo que no necesitas descargar nada adicional.

---

## 📥 Cómo Instalar

1. Elige la carpeta según el cargador de mods que uses (**Fabric**, **Forge** o **NeoForge**) y la versión de tu Minecraft.
2. Copia el archivo `.jar` correspondiente.
3. Pégalo en tu carpeta de mods de Minecraft:
   - En Windows: Presiona `Win + R`, escribe `%appdata%\.minecraft\mods` y presiona Enter.
4. *Nota para usuarios de Fabric:* Recuerda tener instalado **Fabric API** correspondiente a tu versión en la carpeta `mods`.

---

## 🕹 Cómo Jugar

### Como Anfitrión (Host):
1. Inicia Minecraft (con Steam abierto).
2. Entra a tu mundo en modo un jugador (Singleplayer).
3. Presiona `Esc` para abrir el menú de pausa y selecciona **"Abrir en LAN"** (o *"Open to LAN"*).
4. Elige los ajustes de juego y haz clic en **"Iniciar mundo LAN"**.
5. ¡El mod detectará automáticamente el servidor y abrirá la sala en Steam!
6. En el menú de pausa verás el botón **"Invitar a Steam"**, o simplemente abre el Overlay de Steam (`Shift + Tab`), haz clic derecho sobre tu amigo e invítalo a jugar.

### Para Unirse (Invitado):
1. Asegúrate de tener Minecraft abierto con la misma versión del juego y los mismos mods del host.
2. Ve a la pantalla de **"Multijugador"**.
3. En la parte superior verás un botón llamado **"Amigos de Steam"**.
4. Haz clic en él: aparecerán tus amigos de Steam que tengan un mundo abierto. Haz clic en **"Unirse"**.
5. *Alternativa:* Si tu amigo te mandó una invitación de Steam, haz clic en **"Unirse a la partida"** directamente desde el chat o la notificación de Steam.

---

## ☕ Donaciones / Support the Project

Si este mod te sirvió para jugar con tus amigos y deseas apoyar el desarrollo y futuras actualizaciones, ¡cualquier donación es bienvenida y enormemente agradecida!

* **Litecoin (LTC):**
  `ltc1q90hfyjnzmxjevcrcuy6cta2hgj7v5w0n29unun`  
  *(Red: LTC)*

* **USDT (Tether):**
  `0x791DA670fe512e63E4e30F549c0262d49A2cBA2d`  
  *(Red: BNB Smart Chain - BEP20)*

* **BNB:**
  `0x791DA670fe512e63E4e30F549c0262d49A2cBA2d`  
  *(Red: BNB Smart Chain - BEP20)*

---

## Disclaimer
SteamCraft is an unofficial open-source modification for Minecraft. 
It is not affiliated with, endorsed by, or associated with Mojang Studios, Microsoft, or Valve Corporation.
Minecraft is a trademark of Mojang AB. Steam and Spacewar are trademarks of Valve Corporation.
