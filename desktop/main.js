const { app, BrowserWindow, shell, powerSaveBlocker, Menu } = require('electron');
const path = require('path');
const fs = require('fs');

let cfg = {};
try { cfg = JSON.parse(fs.readFileSync(path.join(__dirname, 'config.json'), 'utf8')); } catch (e) {}

if (cfg.autoplay) app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');
if (cfg.id) app.setAppUserModelId(cfg.id);

let win = null;

function createWindow() {
  Menu.setApplicationMenu(null);
  const size = cfg.orient === 'portrait' ? [480, 820] : cfg.orient === 'landscape' ? [1100, 640] : [1100, 760];
  win = new BrowserWindow({
    width: size[0],
    height: size[1],
    minWidth: 320,
    minHeight: 300,
    title: cfg.name || 'App',
    backgroundColor: cfg.bg || '#FFFFFF',
    icon: path.join(__dirname, 'icon.png'),
    fullscreen: !!(cfg.fullscreen || cfg.immersive),
    autoHideMenuBar: true,
    show: false,
    webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true, backgroundThrottling: false }
  });

  const wc = win.webContents;
  const ses = wc.session;

  ses.setPermissionRequestHandler(function (_w, permission, cb) {
    if (permission === 'media') return cb(!!(cfg.cam || cfg.mic));
    if (permission === 'geolocation') return cb(!!cfg.loc);
    cb(['fullscreen', 'clipboard-sanitized-write', 'clipboard-read', 'pointerLock', 'notifications'].indexOf(permission) >= 0);
  });
  ses.setPermissionCheckHandler(function (_w, permission) {
    if (permission === 'media') return !!(cfg.cam || cfg.mic);
    if (permission === 'geolocation') return !!cfg.loc;
    return true;
  });

  if (cfg.internet === false) {
    ses.webRequest.onBeforeRequest({ urls: ['http://*/*', 'https://*/*', 'ws://*/*', 'wss://*/*'] }, function (_d, cb) { cb({ cancel: true }); });
  }

  wc.setWindowOpenHandler(function (d) {
    if (/^https?:/i.test(d.url) && cfg.external) { shell.openExternal(d.url); return { action: 'deny' }; }
    return { action: 'allow' };
  });
  wc.on('will-navigate', function (e, url) {
    if (/^https?:/i.test(url) && cfg.external) { e.preventDefault(); shell.openExternal(url); }
  });

  if (cfg.noZoom) wc.setVisualZoomLevelLimits(1, 1);
  wc.on('before-input-event', function (e, input) {
    if (input.type !== 'keyDown') return;
    if (input.key === 'F11') { win.setFullScreen(!win.isFullScreen()); e.preventDefault(); return; }
    if (cfg.noZoom && input.control && ['+', '-', '=', '0'].indexOf(input.key) >= 0) e.preventDefault();
  });
  if (cfg.noSelect) {
    wc.on('dom-ready', function () {
      wc.insertCSS('*{-webkit-user-select:none;user-select:none} input,textarea,[contenteditable]{-webkit-user-select:text;user-select:text}');
    });
  }

  win.on('page-title-updated', function (e) { e.preventDefault(); });
  win.once('ready-to-show', function () { win.show(); });
  win.loadFile(path.join(__dirname, 'app', 'index.html'));
  if (cfg.keepAwake) powerSaveBlocker.start('prevent-display-sleep');
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', function () { if (win) { if (win.isMinimized()) win.restore(); win.focus(); } });
  app.whenReady().then(createWindow);
  app.on('window-all-closed', function () { app.quit(); });
}
