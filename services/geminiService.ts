
import { GoogleGenAI } from "@google/genai";

const API_KEY = process.env.API_KEY || '';

export const getGeminiResponse = async (
  prompt: string, 
  history: { role: string; parts: { text: string }[] }[],
  location?: { latitude: number; longitude: number }
) => {
  const ai = new GoogleGenAI({ apiKey: API_KEY });
  
  const systemInstruction = `
    Te egy Senior Android Fejlesztő és Megoldásépítész vagy. 
    A specialitásod a helyalapú (Google Maps) alkalmazások fejlesztése a magyar piacra.
    
    FELADATOD:
    Segíts a felhasználónak egy játszótérkereső Android alkalmazást (Playground Finder) építeni. 
    Célországok: Magyarország, Horvátország, Ausztria, Szlovénia és Szlovákia.
    
    SZABÁLYOK:
    1. KIZÁRÓLAG MAGYAR NYELVEN válaszolj.
    2. Használj modern Android technológiákat: Kotlin, Jetpack Compose, Material 3, Coroutines, Flow, Hilt.
    3. Adj konkrét kódmintákat a Google Maps SDK és Places API integrációhoz.
    4. Amikor konkrét helyszínekről vagy játszóterekről van szó, használd a googleMaps eszközt.
    5. Ha grounding (keresési) adatokat kapsz, azokat építsd be a válaszba és sorold fel a forrásokat/linkeket a válasz végén.
    6. A kódblokkokat jelöld Markdown szintaxissal (\`\`\`kotlin ... \`\`\`).
  `;

  try {
    const config: any = {
      systemInstruction,
      temperature: 0.7,
      tools: [{ googleMaps: {} }, { googleSearch: {} }],
    };

    if (location) {
      config.toolConfig = {
        retrievalConfig: {
          latLng: {
            latitude: location.latitude,
            longitude: location.longitude
          }
        }
      };
    }

    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: [
        ...history,
        { role: 'user', parts: [{ text: prompt }] }
      ],
      config,
    });

    const text = response.text;
    const groundingMetadata = response.candidates?.[0]?.groundingMetadata;
    const groundingChunks = groundingMetadata?.groundingChunks || [];
    
    let finalOutput = text || "Sajnálom, nem sikerült választ generálnom.";
    
    if (groundingChunks.length > 0) {
      const links = groundingChunks
        .map((chunk: any) => {
          if (chunk.maps) return `[${chunk.maps.title}](${chunk.maps.uri})`;
          if (chunk.web) return `[${chunk.web.title}](${chunk.web.uri})`;
          return null;
        })
        .filter(Boolean);
        
      if (links.length > 0) {
        // Remove duplicates
        const uniqueLinks = Array.from(new Set(links));
        finalOutput += "\n\n**Helyszínek és Források:**\n" + uniqueLinks.map(link => `- ${link}`).join('\n');
      }
    }

    return finalOutput;
  } catch (error) {
    console.error("Gemini API Error:", error);
    return "Hiba történt a kapcsolódás során. Kérlek, ellenőrizd a hálózatot és próbáld újra.";
  }
};
