# 🎮 SteamCraft - Multijugador P2P de Minecraft vía Steam (Spacewar)

**SteamCraft** es un mod para Minecraft que permite jugar multijugador con tus amigos utilizando la red Peer-to-Peer (P2P) de **Steam** mediante el **AppID 480 (Spacewar)**. 

### ✨ Ventajas:
* **Sin abrir puertos ni configurar routers** (cero Port Forwarding).
* **Sin programas externos** como Hamachi, Radmin VPN o ngrok.
* **Túnel directo y seguro** mediante los relays y sockets de Steam Datagram Relay (SDR).
* **Integración total con la lista de amigos y el overlay de Steam (Shift + Tab)**.

---

## 📁 Versiones Listas para Descargar y Compartir

Los archivos están organizados por plataforma y versión en sus respectivas carpetas:

```
SteamCraft-Multiplayer/
├── Fabric/
│   ├── 1.19.2/
│   │   └── steamcraft-1.19.2-fabric.jar
│   ├── 1.20.1/
│   │   └── steamcraft-1.20.1-fabric.jar
│   └── 1.21.1/
│       └── steamcraft-1.21.1-fabric.jar
├── Forge/
│   └── 1.20.1/
│       └── steamcraft-1.20.1-forge.jar
└── NeoForge/
    └── 1.21.1/
        └── steamcraft-1.21.1-neoforge.jar
```

> **Nota técnica:** A partir de Minecraft 1.20.6 / 1.21, la comunidad principal de Forge migró a **NeoForge**, por lo que para 1.21.1 dispones de la versión nativa para **NeoForge** y **Fabric**.

---

## 📌 Requisitos Previos

1. **Steam abierto en segundo plano:**
   - Tanto la persona que crea el mundo (Host) como quienes se unan deben tener el cliente de Steam abierto y haber iniciado sesión en su cuenta.
   - Ambas personas deben tenerse agregadas mutuamente como amigos en Steam.
2. **Librerías nativas:**
   - Todos los archivos `.jar` de SteamCraft ya vienen con las librerías nativas empaquetadas (`steam_api64.dll` y `steamworks4j64.dll`), por lo que no necesitas descargar nada adicional.

---

## 🚀 Cómo Instalar

1. Elige la carpeta según el cargador de mods que uses (**Fabric**, **Forge** o **NeoForge**) y la versión de tu Minecraft.
2. Copia el archivo `.jar` correspondiente.
3. Pégalo en tu carpeta de mods de Minecraft:
   - En Windows: Presiona `Win + R`, escribe `%appdata%\.minecraft\mods` y presiona Enter.
4. *Nota para usuarios de Fabric:* Recuerda tener instalado **Fabric API** correspondiente a tu versión en la carpeta `mods`.

---

## 🕹️ Cómo Jugar

### Como Anfitrión (Host):
1. Inicia Minecraft (con Steam abierto).
2. Entra a tu mundo en modo un jugador (Singleplayer).
3. Presiona `Esc` para abrir el menú de pausa y selecciona **"Abrir en LAN"** (o *"Open to LAN"*).
4. Elige los ajustes de juego y haz clic en **"Iniciar mundo LAN"**.
5. ¡El mod detectará automáticamente el servidor y abrirá la sala en Steam!
6. En el menú de pausa verás el botón **"Invitar amigos de Steam"**, o simplemente abre el Overlay de Steam (`Shift + Tab`), haz clic derecho sobre tu amigo e invítalo a jugar.

### Para Unirse (Invitado):
1. Asegúrate de tener Minecraft abierto con la misma versión del juego y los mismos mods del host.
2. Ve a la pantalla de **"Multijugador"**.
3. En la parte superior verás un botón llamado **"Amigos de Steam"**.
4. Haz clic en él: aparecerán tus amigos de Steam que tengan un mundo abierto. Haz clic en **"Unirse"**.
5. *Alternativa:* Si tu amigo te mandó una invitación de Steam, haz clic en **"Unirse a la partida"** directamente desde el chat o la notificación de Steam.

---

## ⚠️ ¿Por qué Spacewar (AppID 480)?
Steam utiliza el ID 480 (Spacewar) como entorno oficial de pruebas para desarrolladores. Gracias a esto, Steam permite inicializar Steamworks, gestionar lobbies P2P, transferir paquetes de red e invitar amigos sin necesidad de pagar una tarifa de desarrollador ni registrar un juego comercial propio en la tienda de Steam. Cuando juegues, tus amigos verán en Steam que estás jugando a *"Spacewar"*.
