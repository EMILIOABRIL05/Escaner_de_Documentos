package io.github.emilioabril05.folio

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class AppManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `no declara el permiso de identificador de publicidad`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = packageInfo.requestedPermissions?.toList().orEmpty()

        assertFalse(AD_ID_PERMISSION in permissions)
    }

    @Test
    fun `desactiva la copia de seguridad en la nube`() {
        val allowsBackup = context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP

        assertEquals(0, allowsBackup)
    }

    @Test
    fun `el manifiesto usa la configuración de seguridad de red`() {
        val configField = ApplicationInfo::class.java.getField("networkSecurityConfigRes")

        assertEquals(R.xml.network_security_config, configField.getInt(context.applicationInfo))
    }

    @Test
    fun `la configuración de red no permite tráfico sin cifrar`() {
        val parser = context.resources.getXml(R.xml.network_security_config)
        var cleartextPermitted: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "base-config") {
                cleartextPermitted = parser.getAttributeValue(null, "cleartextTrafficPermitted")
            }
        }

        assertEquals("false", cleartextPermitted)
    }

    @Test
    fun `no registra ningún destino para enviar telemetría de Google`() {
        val discovery = ComponentName(context, TRANSPORT_BACKEND_DISCOVERY)
        val metaData =
            context.packageManager.getServiceInfo(discovery, PackageManager.GET_META_DATA).metaData
        val backends = metaData?.keySet().orEmpty().filter { it.startsWith(BACKEND_PREFIX) }

        assertEquals(emptyList<String>(), backends)
    }

    private companion object {
        const val TRANSPORT_BACKEND_DISCOVERY = "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery"
        const val BACKEND_PREFIX = "backend:"
        const val AD_ID_PERMISSION = "com.google.android.gms.permission.AD_ID"
    }
}
