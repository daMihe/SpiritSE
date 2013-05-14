import xml.dom.minidom as dom

doc = dom.parse("AndroidManifest.xml")
newint = int(doc.getElementsByTagName("manifest")[0].getAttribute("android:versionCode"))+1
print("Android Version Updater")
print("Neue Build-Nummer: "+str(newint))
doc.getElementsByTagName("manifest")[0].setAttribute("android:versionCode",str(newint))
#print(str(doc.toxml()))
f = open("AndroidManifest.xml","w")
f.write(str(doc.toxml()))
f.close()
