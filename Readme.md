
SDK de Facebook para Android
Ejecutar suite de prueba con gradle Centro de expertos

Esta biblioteca le permite integrar Facebook en su aplicación de Android.

Obtenga más información sobre las muestras proporcionadas, la documentación, la integración del SDK en su aplicación, el acceso al código fuente y más en https://developers.facebook.com/docs/android

👋¡El equipo de SDK está ansioso por aprender de usted! Complete esta encuesta para decirnos qué es lo más importante para usted y cómo podemos mejorar.

PRUÉBALO
Consulte los tutoriales disponibles en línea en https://developers.facebook.com/docs/android/getting-started
¡Empieza a codificar! Visite https://developers.facebook.com/docs/android/ para obtener tutoriales y documentación de referencia.
CARACTERISTICAS
Acceso
Intercambio
Mensajero
Enlaces de aplicaciones
Analítica
API de gráfico
Marketing
ESTRUCTURA
El SDK está separado en módulos con la siguiente estructura.

+----------------------------------------------------+
|                                                    |
| Facebook-android-sdk                               |
|                                                    |
+----------------------------------------------------+
+----------+ +----------+ +------------+ +-----------+
|          | |          | |            | |           |
| Facebook | | Facebook | | Facebook   | | Facebook  |
| -Login : | | -Share   | | -Messenger | | -Applinks |
|          | |          | |            | |           |
+----------+ +----------+ |            | |           |
+-----------------------+ |            | |           |
|                       | |            | |           |
| Facebook-Common       | |            | |           |
|                       | |            | |           |
+-----------------------+ +------------+ +-----------+
+----------------------------------------------------+
|                                                    |
| Facebook-Core                                      |
|                                                    |
+----------------------------------------------------+
USO
Los SDK de Facebook se dividen en módulos separados, como se muestra arriba. Para garantizar el uso más optimizado del espacio, instale solo los módulos que pretende utilizar. Para comenzar, consulte la sección Instalación a continuación.

Cualquier inicialización del SDK de Facebook debe ocurrir solo en el proceso principal de la aplicación. No se admite el uso del SDK de Facebook en procesos que no sean el proceso principal y es probable que cause problemas.

INSTALACIÓN
Los SDK de Facebook se publican en Maven como módulos independientes. Para utilizar una de las funciones enumeradas anteriormente, incluya la dependencia (o dependencias) apropiadas que se enumeran a continuación en su app/build.gradlearchivo.

dependencies {
    // Facebook Core only (Analytics)
    implementation 'com.facebook.android:facebook-core:latest.release'

    // Facebook Login only
    implementation 'com.facebook.android:facebook-login:latest.release'

    // Facebook Share only
    implementation 'com.facebook.android:facebook-share:latest.release'

    // Facebook Messenger only
    implementation 'com.facebook.android:facebook-messenger:latest.release'

    // Facebook App Links only
    implementation 'com.facebook.android:facebook-applinks:latest.release'

    // Facebook Android SDK (everything)
    implementation 'com.facebook.android:facebook-android-sdk:latest.release'
}
Es posible que también deba agregar lo siguiente a su archivo project/build.gradle.

buildscript {
    repositories {
        mavenCentral()
    }
}
DAR OPINION
Informe errores o problemas a https://developers.facebook.com/bugs/

También puede visitar nuestro Foro de la comunidad de desarrolladores de Facebook , unirse al Grupo de desarrolladores de Facebook en Facebook , hacer preguntas sobre Stack Overflow o abrir un problema en este repositorio.

SEGURIDAD
Consulte la POLÍTICA DE SEGURIDAD para obtener más información sobre nuestro programa de recompensas por errores.

CONTRIBUYENDO
Podemos aceptar contribuciones al SDK de Facebook para Android. Para contribuir por favor haga lo siguiente.

Siga las instrucciones en CONTRIBUTING.md .
Envíe su solicitud de extracción a la sucursal principal . Esto nos permite fusionar su cambio en nuestro principal interno y luego sacar el cambio en la próxima versión.
LICENCIA
Salvo que se indique lo contrario, el SDK de Facebook para Android se otorga bajo la Licencia de plataforma de Facebook ( https://github.com/facebook/facebook-android-sdk/blob/main/LICENSE.txt ).

A menos que lo requiera la ley aplicable o se acuerde por escrito, el software distribuido bajo la Licencia se distribuye "TAL CUAL", SIN GARANTÍAS NI CONDICIONES DE NINGÚN TIPO, ya sea expresa o implícita. Consulte la Licencia para conocer el idioma específico que rige los permisos y las limitaciones en virtud de la Licencia.

TÉRMINOS DE DESARROLLADOR
Al habilitar las integraciones de Facebook, incluso a través de este SDK, puede compartir información con Facebook, incluida información sobre el uso de su aplicación por parte de las personas. Facebook usará la información recibida de acuerdo con nuestra Política de uso de datos ( https://www.facebook.com/about/privacy/ ), incluso para brindarle información sobre la efectividad de sus anuncios y el uso de su aplicación. Estas integraciones también nos permiten a nosotros y a nuestros socios publicar anuncios dentro y fuera de Facebook.

Puede limitar el intercambio de información con nosotros actualizando el control Insights en la herramienta para desarrolladores ( https://developers.facebook.com/apps/[app_id]/settings/advanced ).

Si utiliza una integración de Facebook, incluso para compartir información con nosotros, acepta y confirma que proporcionó un aviso apropiado y suficientemente destacado y obtuvo el consentimiento apropiado de sus usuarios con respecto a dicha recopilación, uso y divulgación (incluidos, como mínimo , a través de su política de privacidad). Además, acepta que no compartirá información con nosotros sobre niños menores de 13 años.

Usted acepta cumplir con todas las leyes y regulaciones aplicables y también acepta nuestros Términos ( https://www.facebook.com/policies/ ), incluidas nuestras Políticas de plataforma ( https://developers.facebook.com/policy/ ) y Pautas de publicidad, según corresponda ( https://www.facebook.com/ad_guidelines.php ).

Al usar el SDK de Facebook para Android, aceptas estos términos.

Lanzamientos 53
Facebook SDK sdk-versión-15.1.0
Más reciente
el 1 de noviembre
