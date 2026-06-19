# 🎧 The VVF Sound Booster & Equalizer

**The VVF Sound Booster** एक अत्याधुनिक, बिल्कुल मुफ्त और ओपन-सोर्स सिस्टम-वाइड ऑडियो ऑप्टिमाइज़ेशन एप्लीकेशन है। यह आपके एंड्रॉइड डिवाइस की आवाज़ को एक नए स्तर पर ले जाने के लिए एडवांस्ड ऑडियो फीचर्स प्रदान करता है।

---

## 🌟 मुख्य विशेषताएं (Core Features)

### 🎧 उन्नत ऑडियो प्रोसेसर (Core Audio Effects)
- **10-बैंड ग्राफिक इक्वलाइज़र (10-Band Graphic EQ):** अपनी पसंद के अनुसार हर फ्रीक्वेंसी को बारीकी से ट्यून करें।
- **ऑटोईक्यु सपोर्ट (AutoEq Integration):** दुनिया भर के हजारों हेडफोन मॉडल्स के लिए ऑटो-ट्यून्ड प्रोफाइल्स।
- **बास बूस्टर और वर्चुअलाइज़र (Bass & Virtualizer):** गहरे बास और 3D सराउंड साउंड का सजीव अनुभव।
- **डायनामिक लिमिटर (Dynamic Limiter):** हाई-वॉल्यूम पर भी बिना किसी डिस्टॉर्शन (Distortion) के क्रिस्टल क्लियर साउंड।

### 🇮🇳 समृद्ध भारतीय संगीत प्रीसेट्स (Rich Indian Presets)
विशेष रूप से भारतीय श्रोताओं और वाद्ययंत्रों के लिए अनुकूलित किए गए सुंदर विजुअल आइकनों और टैग्स के साथ विशेष प्रोफाइल्स:
- **शास्त्रीय (Indian Classical - 🪕):** सितार, सरोद और सुरों की बारीकियों को उभारने के लिए विशेष ट्यूनिंग।
- **सूफ़ी और ग़ज़ल (Sufi & Qawwali - 🎤):** रूहानी आवाज़ और वोकल्स को स्पष्ट और गहरा बनाने के लिए।
- **बॉलीवुड और ढोल (Bollywood Dhamaaka - 🥁):** ढोलक, तबला और हाई-एनर्जी बीट्स को दमदार बास देने के लिए।
- **भक्ति और आरती (Devotional / Bhakti - 🪔):** भजनों, मंत्रों और घंटियों की रीवर्ब और क्लेरिटी को बढ़ाने के लिए।

### 🛡️ रियल-टाइम सर्विस गार्डियन और रिसोर्स मॉनिटर 
पृष्ठभूमि (Background) में चलने वाली ऑडियो सर्विस के लिए एक लाइव डैशबोर्ड:
- **रैम (RAM) और सीपीयू (CPU) उपयोग:** रीयल-टाइम सर्विस रिसोर्स ट्रैकिंग।
- **सेल्फ-हीलिंग गार्डियन (Self-Healing Guardian):** हाई-मेमोरी के दौरान ऑटो-जीसी (Auto-GC) द्वारा बैकग्राउंड क्रैश से सुरक्षा कवच।
- **इंजन हेल्थ स्टेटस:** लाइव 'Healthy' व 'Degraded' स्टेटस इंडिकेटर।

---

## 🛠️ तकनीकी विवरण (Tech Stack)
- **प्रोग्रामिंग भाषा:** Kotlin
- **यूआई फ्रेमवर्क:** Jetpack Compose (Material 3 डिज़ाइन सिस्टम)
- **आर्किटेक्चर:** MVVM (Model-View-ViewModel) + Coroutines Flow
- **डेटाबेस:** Local Room Database (फॉर कस्टमाइज़्ड यूजर प्रोफाइल्स)
- **बैकग्राउंड सर्विस:** Foreground Audio Service align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/a54a1d13-70e3-4f0b-940b-12a1fd2a0cc7

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
