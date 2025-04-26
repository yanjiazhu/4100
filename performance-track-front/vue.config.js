module.exports = {
  lintOnSave: false,
  devServer: {
    port: 8081,
    client: {
      overlay: true,
      progress: true
    },
    webSocketServer: false  // 禁用 WebSocket
  }
}
