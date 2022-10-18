const Bonjour = require('bonjour')
const bonjour = new Bonjour()

const AirPlay = require('airplay-protocol')

bonjour.find({ type: 'airplay' }, function (service) {
  console.log(JSON.stringify({...service}, null, 4))

  const player = new AirPlay(service.host, service.port)

  player.play('https://www.cbsnews.com/common/video/cbsn_header_prod.m3u8', 0)
})
