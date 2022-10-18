const Bonjour = require('bonjour')
const bonjour = new Bonjour()

const AirPlay = require('airplay-protocol')

bonjour.find({ type: 'airplay' }, function (service) {
  console.log(JSON.stringify({...service}, null, 4))

  const host = (service.addresses.length)
    ? service.addresses[0]
    : (service.referer && service.referer.address)
      ? service.referer.address
      : service.host
  const port = service.port
  console.log({host, port, name: service.name})

  const player = new AirPlay(host, port)

  player.play('https://www.cbsnews.com/common/video/cbsn_header_prod.m3u8', 0)
})
