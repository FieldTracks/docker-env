// This file can be replaced during build by using the `fileReplacements` array.
// `ng build ---prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  mqtt_url: 'ws://localhost:8883/',
  uuid: 'fd:a5:06:93:a4:e2:4f:b1:af:cf:c6:eb:07:64:78:25',
  grafana_base: 'https://local-dev.fieldtracks.org:8443/grafana/d/Stones/stones?orgId=1&var-node='
};

/*
 * In development mode, to ignore zone related error stack frames such as
 * `zone.run`, `zoneDelegate.invokeTask` for easier debugging, you can
 * import the following file, but please comment it out in production mode
 * because it will have performance impact when throw error
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
