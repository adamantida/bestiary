import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.Map;

public class NPCXmlGenerator {

    public Document createXMLDocument() throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        // Root element
        Element root = document.createElement("root");
        root.setAttribute("version", "4.5");
        root.setAttribute("dataversion", "20230911");
        root.setAttribute("release", "8.1|CoreRPG:6");
        document.appendChild(root);

        // Create library and bestiary elements
        Element library = document.createElement("library");
        Element bestiary = document.createElement("bestiary");
        bestiary.setAttribute("static", "true");
        library.appendChild(bestiary);

        // Add categoryname, name, and entries elements
        createTextNode(bestiary, "categoryname", "", document, "string");
        createTextNode(bestiary, "name", "bestiary", document, "string");
        Element entries = document.createElement("entries");
        bestiary.appendChild(entries);

        // Adding the initial NPC (meta information)
        Element npcElement = createNPCEntry(document, "NPCs", "npc");
        entries.appendChild(npcElement);

        root.appendChild(library);

        // Add a new <npc> element after </library>
        Element npcRoot = document.createElement("npc");
        root.appendChild(npcRoot);

        return document;
    }

    private Element createNPCEntry(Document document, String npcName, String recordType) {
        Element npc = document.createElement("npc");

        Element libraryLink = document.createElement("librarylink");
        libraryLink.setAttribute("type", "windowreference");

        createTextNode(libraryLink, "class", "reference_list", document, null);
        createTextNode(libraryLink, "recordname", "..", document, null);
        npc.appendChild(libraryLink);

        createTextNode(npc, "name", npcName, document, "string");
        createTextNode(npc, "recordtype", recordType, document, "string");

        return npc;
    }

    public void addNPC(Document document, NPC npc, int npcId) {
        // Find the <npc> element after </library> to append new NPC entries
        Element npcRoot = (Element) document.getElementsByTagName("npc").item(1); // This is the second <npc> tag

        Element npcElement = document.createElement("id-" + String.format("%05d", npcId));
        npcRoot.appendChild(npcElement);

        // Add abilities
        Element abilities = document.createElement("abilities");
        npcElement.appendChild(abilities);
        addAbility(abilities, "strength", npc.Strength, document);
        addAbility(abilities, "dexterity", npc.Dexterity, document);
        addAbility(abilities, "constitution", npc.Constitution, document);
        addAbility(abilities, "intelligence", npc.Intellect, document);
        addAbility(abilities, "wisdom", npc.Wisdom, document);
        addAbility(abilities, "charisma", npc.Charisma, document);

        // Add other attributes
        //<damageresistances type="string">acid, cold, fire, lightning, thunder</damageresistances>
        //<damageimmunities type="string">lightning</damageimmunities>
        //<conditionimmunities type="string">blinded</conditionimmunities>

        //innatespells
        //
        //<savingthrows type="string">Con +6, Int +8, Wis +6</savingthrows>
        if (npc.savingThrows != null)
            createTextNode(npcElement, "savingthrows", String.join(", ", npc.savingThrows), document, "string");

        //Resist
        if (npc.damageResistances != null)
            createTextNode(npcElement, "damageresistances", npc.damageResistances, document, "string");
        if (npc.damageImmunities != null)
            createTextNode(npcElement, "damageimmunities", npc.damageImmunities, document, "string");
        if (npc.conditionImmunities != null)
            createTextNode(npcElement, "conditionimmunities", npc.conditionImmunities, document, "string");
        ///
        createTextNode(npcElement, "ac", Integer.toString(npc.AC), document, "number");
        if (npc.AC_description != null) createTextNode(npcElement, "actext", npc.AC_description, document, "string");
        createTextNode(npcElement, "hp", Integer.toString(npc.HP), document, "number");
        createTextNode(npcElement, "hd", npc.HP_formula, document, "string");
        createTextNode(npcElement, "name", npc.name, document, "string");
        createTextNode(npcElement, "size", npc.size, document, "string");
        createTextNode(npcElement, "type", npc.type, document, "string");
        //<alignment type="string">Neutral Good</alignment>
        createTextNode(npcElement, "alignment", npc.Alignment, document, "string");
        createTextNode(npcElement, "speed", String.join(" ft., ", npc.speed).replace("=", " ") + " ft.", document, "string");
        //<senses type="string">passive Perception 15, darkvision 120 ft.</senses>
        createTextNode(npcElement, "senses", npc.passivePerception + "; " + String.join(", ", npc.senses), document, "string");
        //<languages type="string">Auran, Aarakocra</languages>
        if (npc.languages != null) {
            createTextNode(npcElement, "languages", String.join(", ", npc.languages), document, "string");
        }

        //<skills type="string">perception +5</skills>
        if (npc.skills != null) {
            createTextNode(npcElement, "skills", String.join(", ", npc.skills), document, "string");
        }

        // Add actions and traits
        if (npc.actions != null) addFeatures(npcElement, "actions", npc.actions, document);
        if (npc.bonusActions != null) addFeatures(npcElement, "bonusactions", npc.bonusActions, document);
        if (npc.legendaryActions != null) addFeatures(npcElement, "legendaryactions", npc.legendaryActions, document);
        if (npc.reactions != null) addFeatures(npcElement, "reactions", npc.reactions, document);
        addFeatures(npcElement, "traits", npc.traits, document);
        if (npc.innateSpellcasting != null) {
            addInnateSpellcasting(npcElement, npc.innateSpellcasting, document);
        }
        if (npc.lairActions != null) {
            addLairActions(npcElement, npc.lairActions, document);
        }

        //image
        //<picture type="token">images/Aboleth.webp@SWKARTFANTASYMONSTERSFM1</picture>
        if (npc.PortraitPath != null && !npc.PortraitPath.equals("portraits/null"))
            createTextNode(npcElement, "picture", "all/" + npc.PortraitPath, document, "token");
        //<token type="token">tokens/Aboleth.webp@SWKARTFANTASYMONSTERSFM1</token>
        if (npc.TokenPath != null && !npc.TokenPath.equals("tokens/null"))
            createTextNode(npcElement, "token", "all/" + npc.TokenPath, document, "token");
        //token3Dflat type="token"></token3Dflat>
        createTextNode(npcElement, "token3Dflat", "", document, "token");
        //<text type="formattedtext" />


        //Cr and XP <cr type="string">1/4</cr> <xp type="number">0</xp>
        createTextNode(npcElement, "cr", npc.cr, document, "string");
        createTextNode(npcElement, "xp", Integer.toString(npc.xp), document, "number");

        if (npc.description != null)
            createTextNode(npcElement, "text", npc.description, document, "formattedtext");
    }

    private void addLairActions(Element parent, Liar liar, Document document) {
        // Создаем элемент <lairactions>
        Element lairActionsElement = document.createElement("lairactions");

        // Добавляем описание, если оно присутствует
        if (liar.description != null && !liar.description.isEmpty()) {
            Element descriptionElement = document.createElement("id-" + String.format("%05d", 1));
            createTextNode(descriptionElement, "name", "Options", document, "string");
            createTextNode(descriptionElement, "desc", liar.description, document, "string");
            lairActionsElement.appendChild(descriptionElement);
        }

        // Добавляем каждый элемент lairActions
        int idCounter = liar.description != null ? 2 : 1;  // Если есть описание, начинаем с 2
        if (liar.lairActions != null) {
            for (String lairAction : liar.lairActions) {
                Element actionElement = document.createElement("id-" + String.format("%05d", idCounter++));
                createTextNode(actionElement, "name", lairAction, document, "string");
                createTextNode(actionElement, "desc", "description", document, "string"); // Добавьте описание, если оно доступно
                lairActionsElement.appendChild(actionElement);
            }
        }

        // Добавляем каждый item из itemsName
        if (liar.itemsName != null) {
            for (Map.Entry<String, List<String>> entry : liar.itemsName.entrySet()) {
                Element itemElement = document.createElement("id-" + String.format("%05d", idCounter++));
                createTextNode(itemElement, "name", entry.getKey(), document, "string");
                for (String entryText : entry.getValue()) {
                    createTextNode(itemElement, "desc", entryText, document, "string");
                }
                lairActionsElement.appendChild(itemElement);
            }
        }

        parent.appendChild(lairActionsElement);
    }


    private void addInnateSpellcasting(Element parent, InnateSpellcasting spellcasting, Document document) {
        // Создаем элемент <innatespells>
        Element innatespellsElement = document.createElement("innatespells");

        // Добавляем заклинания из will
        int idCounter = 1;
        for (String spell : spellcasting.will) {
            Element spellElement = document.createElement("id-" + String.format("%05d", idCounter++));
            createTextNode(spellElement, "name", spell + " (At will)", document, "string");
            createTextNode(spellElement, "desc", "description", document, "string");
            innatespellsElement.appendChild(spellElement);
        }

        // Добавляем заклинания из daily
        for (Map.Entry<String, List<String>> entry : spellcasting.daily.entrySet()) {
            String uses = entry.getKey();
            String usesText = getUsesText(uses);
            for (String spell : entry.getValue()) {
                Element spellElement = document.createElement("id-" + String.format("%05d", idCounter++));
                createTextNode(spellElement, "name", spell + " (" + usesText + ")", document, "string");
                createTextNode(spellElement, "desc", "description", document, "string");
                innatespellsElement.appendChild(spellElement);
            }
        }

        parent.appendChild(innatespellsElement);
    }

    private String getUsesText(String uses) {
        if (uses.contains("e")) {
            return uses.replace("e", "/day");
        } else if (uses.contains("-")) {
            return uses + "/day";
        } else {
            return uses + "/day";
        }
    }

    private void addAbility(Element parent, String abilityName, int score, Document document) {
        Element ability = document.createElement(abilityName);
        createTextNode(ability, "bonus", Integer.toString((score - 10) / 2), document, "number");
        createTextNode(ability, "score", Integer.toString(score), document, "number");
        parent.appendChild(ability);
    }

    private void addFeatures(Element parent, String featureType, EntityFeature[] features, Document document) {
        Element featureElement = document.createElement(featureType);
        for (int i = 0; i < features.length; i++) {
            Element feature = document.createElement("id-" + String.format("%05d", i + 1));
            createTextNode(feature, "name", features[i].name, document, "string");
            createTextNode(feature, "desc", features[i].description, document, "string");
            featureElement.appendChild(feature);
        }
        parent.appendChild(featureElement);
    }

    // Modified createTextNode method to accept an optional type attribute
    private Element createTextNode(Element parent, String tagName, String text, Document document, String typeAttr) {
        Element element = document.createElement(tagName);
        if (typeAttr != null) {
            element.setAttribute("type", typeAttr);
        }
        element.appendChild(document.createTextNode(text));
        parent.appendChild(element);
        return element;
    }

    public void saveDocument(Document document, String filepath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(filepath));

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(domSource, streamResult);
    }
}
