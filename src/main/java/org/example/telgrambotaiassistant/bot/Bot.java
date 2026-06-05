package org.example.telgrambotaiassistant.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.example.telgrambotaiassistant.service.GenerateImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bot extends TelegramLongPollingBot {
    @Autowired
    @Qualifier(value = "restTemplateTelegramApi")
    RestTemplate restTemplate;
    @Autowired
    @Qualifier(value = "imageStorageBean")
    ImageStorage imageStorage;
    @Autowired
    GenerateImageService generateImageService;

    String nameBot;
    String tokenBot;

//    final String PROMPT_GENERATE_IMAGE = "You are an expert in automotive image editing. Your task is to replace the wheels" +
//            "on the car from the first image with the wheels from the second image." +
//            "Requirements:" +
//            "1. Keep the car EXACTLY the same - same color, same body, same lighting, same background, same angle, same reflections" +
//            "2. ONLY change the wheels - remove the existing wheels completely and install the new wheels from the second image" +
//            "3. The new wheels must:" +
//            "- Match the perspective and angle of the car" +
//            "- Be properly aligned with the wheel arches" +
//            "- Have realistic shadows and lighting consistent with the scene" +
//            "- Look naturally installed (not \"photoshopped\" or floating)" +
//            "4. Preserve the tire thickness and proportions appropriate for this car model" +
//            "5. The final image should look like a real photograph, not an illustration" +
//            "Output: A single high-quality image showing the car with the new wheels installed." +
//            "Do not add any text, watermarks, or annotations.";

    final String NEW_PROMPT_GENERATE_IMAGE_1_1
            = "You are a professional automotive photo editor specializing in precise wheel replacement using reference-based image transfer." +
            "TASK:" +
            "Replace ONLY the wheels of the first image (car) with the wheels from the second image (reference wheels)." +
            "CRITICAL REQUIREMENTS (ABSOLUTE RULES):" +
            "1. The second image wheels are a STRICT VISUAL TEMPLATE." +
            "   You must NOT reinterpret, redesign, or modify them in any way." +
            "2. Preserve wheel geometry EXACTLY:" +
            "- Keep rim shape, spoke count, spoke thickness, curvature, and design identical" +
            "- Do NOT simplify or approximate complex geometry" +
            "- Do NOT “smooth” or “stylize” the wheel design" +
            "3. Transfer wheels as a rigid object:" +
            "- Treat wheels as a physical object being transplanted, not generated" +
            "- Preserve proportions, diameter, and internal structure" +
            "4. Perspective alignment:" +
            "- Match perspective of the car exactly (camera angle, tilt, rotation)" +
            "- Wheels must align perfectly with wheel hubs and arches" +
            "- Maintain correct 3D rotation (no mirrored or flipped designs)" +
            "5. Visual fidelity rules:" +
            "- Keep car body, paint, lighting, shadows, reflections completely unchanged" +
            "- Only replace wheels and tires" +
            "6. No hallucination rule:" +
            "- Do NOT invent new wheel details" +
            "- Do NOT “enhance” or “improve” wheel design" +
            "- Do NOT mix features from other wheels" +
            "7. Realism requirement:" +
            "- Wheels must look physically installed on the car" +
            "- Correct contact with ground, suspension alignment, and shadows" +
            "OUTPUT:" +
            "A single photorealistic image of the car with the exact same wheels from the second image installed, with perfect geometric consistency." +
            "No text, no watermark, no extra objects.";

    final String NEW_PROMPT_GENERATE_IMAGE_1_2 = "You are a high-precision automotive image compositing system." +
            "\n" +
            "TASK:\n" +
            "Replace ONLY the wheels in Image 1 with the wheels from Image 2.\n" +
            "\n" +
            "========================\n" +
            "HARD REFERENCE LOCK LAYER\n" +
            "========================\n" +
            "The wheels from Image 2 are a NON-NEGOTIABLE VISUAL ASSET.\n" +
            "\n" +
            "You MUST treat them as:\n" +
            "- Exact geometric blueprint\n" +
            "- Pixel-accurate reference\n" +
            "- Non-creative, non-stylized object\n" +
            "\n" +
            "Any deviation is considered a failure.\n" +
            "\n" +
            "========================\n" +
            "GEOMETRY PRESERVATION RULES (CRITICAL)\n" +
            "========================\n" +
            "1. Preserve EXACT wheel geometry:\n" +
            "   - spoke count MUST remain identical\n" +
            "   - spoke thickness MUST remain identical\n" +
            "   - rim curvature MUST remain identical\n" +
            "   - internal cutouts MUST remain identical\n" +
            "   - bolt pattern MUST remain identical\n" +
            "\n" +
            "2. ZERO reinterpretation rule:\n" +
            "   - Do NOT redesign wheel structure\n" +
            "   - Do NOT simplify complex shapes\n" +
            "   - Do NOT smooth or “beautify” geometry\n" +
            "   - Do NOT merge or remove spokes\n" +
            "\n" +
            "3. Object transplant rule:\n" +
            "   - Wheels must be treated as rigid objects\n" +
            "   - No generation of “similar wheels” is allowed\n" +
            "\n" +
            "========================\n" +
            "SPATIAL INTEGRITY RULES\n" +
            "========================\n" +
            "- Maintain exact perspective from Image 1\n" +
            "- Match wheel rotation correctly per axle position\n" +
            "- Align wheels precisely with hubs\n" +
            "- Maintain correct tire contact with ground\n" +
            "- Preserve suspension height and stance\n" +
            "\n" +
            "========================\n" +
            "ANTI-HALLUCINATION LAYER\n" +
            "========================\n" +
            "Before finalizing the image:\n" +
            "1. Verify wheel structure matches Image 2 exactly\n" +
            "2. If any uncertainty exists → copy structure more literally, do not approximate\n" +
            "3. Do NOT invent missing visual details\n" +
            "\n" +
            "========================\n" +
            "GLOBAL CONSTRAINTS\n" +
            "========================\n" +
            "- Car body must remain completely unchanged\n" +
            "- Lighting, reflections, shadows must remain unchanged\n" +
            "- Background must remain unchanged\n" +
            "- Only wheels and tires may change\n" +
            "\n" +
            "========================\n" +
            "OUTPUT RULE\n" +
            "========================\n" +
            "Return a single photorealistic image where:\n" +
            "- Wheels are a faithful structural transplant from Image 2\n" +
            "- No geometric deviation exists\n" +
            "- No stylization applied\n" +
            "\n" +
            "No text, no watermark, no additional objects.";
    final String NEW_PROMPT_GENERATE_IMAGE_1_3 =
            "TASK: Perform a wheel replacement on the vehicle.\n" +
                    "\n" +
                    "INPUT IMAGES:\n" +
                    "\n" +
                    "* Reference wheel image(s): the wheel/rim provided by the user.\n" +
                    "* Vehicle image: the car provided by the user.\n" +
                    "\n" +
                    "GOAL:\n" +
                    "Replace the vehicle's existing wheels with the exact wheel shown in the reference image.\n" +
                    "\n" +
                    "STRICT REQUIREMENTS:\n" +
                    "\n" +
                    "1. Use the reference wheel as the source of truth.\n" +
                    "\n" +
                    "   * Do NOT redesign, reinterpret, restyle, improve, enhance, or invent any wheel details.\n" +
                    "   * Preserve the exact spoke design, spoke count, spoke geometry, cutouts, center cap shape, lip shape, bolt pattern appearance, color, finish, texture, and visible details.\n" +
                    "\n" +
                    "2. Identity preservation is critical.\n" +
                    "\n" +
                    "   * The output wheel must be visually identical to the reference wheel.\n" +
                    "   * Copy the wheel design from the reference image, not an approximation.\n" +
                    "\n" +
                    "3. Maintain vehicle identity.\n" +
                    "\n" +
                    "   * Do NOT modify the car body.\n" +
                    "   * Do NOT change paint color.\n" +
                    "   * Do NOT alter lighting setup.\n" +
                    "   * Do NOT add body kits, spoilers, lowering, widebody parts, decals, or any other modifications.\n" +
                    "\n" +
                    "4. Correct wheel fitment.\n" +
                    "\n" +
                    "   * Match wheel size realistically to the vehicle.\n" +
                    "   * Position wheels exactly on the original wheel hubs.\n" +
                    "   * Maintain correct offset and alignment.\n" +
                    "\n" +
                    "5. Perspective accuracy.\n" +
                    "\n" +
                    "   * Adapt the wheel to the vehicle camera angle.\n" +
                    "   * Preserve proper perspective projection.\n" +
                    "   * Preserve realistic foreshortening.\n" +
                    "   * Preserve wheel circular geometry under perspective transformation.\n" +
                    "\n" +
                    "6. Consistency.\n" +
                    "\n" +
                    "   * Front and rear wheels must use the same wheel design.\n" +
                    "   * Do not generate different wheel variants.\n" +
                    "   * Do not mix wheel styles.\n" +
                    "\n" +
                    "7. Photorealism.\n" +
                    "\n" +
                    "   * Match reflections, shadows, brightness, contrast, and environmental lighting of the vehicle image.\n" +
                    "   * Integrate the wheel naturally into the scene.\n" +
                    "\n" +
                    "8. No hallucinations.\n" +
                    "\n" +
                    "   * Do not invent missing wheel details.\n" +
                    "   * Do not create a new wheel design.\n" +
                    "   * Do not substitute a similar wheel.\n" +
                    "   * If the wheel is partially visible in the reference image, preserve all visible design characteristics and replicate only those characteristics consistently.\n" +
                    "\n" +
                    "9. Output requirements.\n" +
                    "\n" +
                    "   * Produce a single realistic image.\n" +
                    "   * The final image must look like the original vehicle photographed with the reference wheel installed.\n" +
                    "   * Priority order:\n" +
                    "     (1) exact wheel identity\n" +
                    "     (2) correct geometry and perspective\n" +
                    "     (3) photorealistic integration\n" +
                    "\n" +
                    "FAILURE CONDITIONS:\n" +
                    "\n" +
                    "* Changed spoke count.\n" +
                    "* Changed spoke shape.\n" +
                    "* Different wheel design on front and rear axle.\n" +
                    "* Altered vehicle appearance.\n" +
                    "* Generated a custom or invented wheel.\n" +
                    "* Incorrect wheel perspective or scale.\n";
    final Logger LOGGER_BOT_SYSTEM = Logger.getLogger(Bot.class.getName());

    public Bot(String botToken, String nameBot) {
        super(botToken);
        this.nameBot = nameBot;
        this.tokenBot = botToken;
    }

    private String getImgPath(String fileId) {
        String url = "https://api.telegram.org/bot" + tokenBot + "/getFile?file_id=" + fileId;
        Map response = restTemplate.getForObject(url, Map.class);
        Map result = (Map) response.get("result");
        return (String) result.get("file_path");
    }

    private byte[] downloadImg(String filePath) {
        String fileUri = "https://api.telegram.org/file/bot" + tokenBot + "/" + filePath;
        return restTemplate.getForObject(fileUri, byte[].class);
    }

    private byte[] getImgUser(Update update) {
        if (update.getMessage().hasPhoto()) {
            List<PhotoSize> photoSizeList = update.getMessage().getPhoto();
            PhotoSize photoSize = photoSizeList.get(photoSizeList.size() - 1);
            String fileId = photoSize.getFileId();
            String pathFile = getImgPath(fileId);
            return downloadImg(pathFile);
        }
        return null;
    }

    @SneakyThrows
    private void errorMessage(Long idChat, String textMessage) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(idChat);
        sendMessage.setText(textMessage);
        execute(sendMessage);
    }

    private byte[] getImgInBaseCode(String base64codeImg) {
        return Base64.getDecoder().decode(base64codeImg);
    }

    @SneakyThrows
    private byte[] getImgFromResponse(String response) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        LOGGER_BOT_SYSTEM.info(
                response.substring(0, 5000)
        );
        JsonNode parts = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts");
        for (JsonNode part : parts) {
            LOGGER_BOT_SYSTEM.info(part.toPrettyString());
            if (part.has("inline_data")) {
                String base64 =
                        part.get("inline_data")
                                .get("data")
                                .asText();

                return Base64.getDecoder().decode(base64);
            }
            if (part.has("inlineData")) {
                String base64 =
                        part.get("inlineData")
                                .get("data")
                                .asText();

                return Base64.getDecoder().decode(base64);
            }
        }
        throw new RuntimeException("No image found in Gemini response");
    }

    @SneakyThrows
    private void sendImageToUser(Long idChat, String response) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(idChat);
        byte[] img = getImgFromResponse(response);
        sendPhoto.setPhoto(
                new InputFile(new ByteArrayInputStream(img), "result.png")
        );
        execute(sendPhoto);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasPhoto()) {
            if (update.hasMessage()) {
                Long idChat = update.getMessage().getChatId();
                errorMessage(idChat, "Отправьте изображение.");
            }
            return;
        }
        Long idChat = update.getMessage().getChatId();
        byte[] img = getImgUser(update);
        if (img == null) {
            errorMessage(idChat, "Не удалось загрузить сообщение.");
            return;
        }
        LOGGER_BOT_SYSTEM.info("Длинна изображения: " + img.length);
        imageStorage.addImgToStorage(idChat, img);
        List<byte[]> images = imageStorage.get(idChat);
        LOGGER_BOT_SYSTEM.info("У текущего пользователя фото: " + images.size());
        if (images.size() >= 2) {
            byte[] img1 = images.get(0);
            byte[] img2 = images.get(1);
            LOGGER_BOT_SYSTEM.info("Получено 2 изображения. Начинается обработка фото...");
            String base64codeImageFirst = Base64.getEncoder().encodeToString(img1);
            String base64codeImageSecond = Base64.getEncoder().encodeToString(img2);
            String response = generateImageService.generateRequest(
                    base64codeImageFirst,
                    base64codeImageSecond,
                    NEW_PROMPT_GENERATE_IMAGE_1_3
            );
            LOGGER_BOT_SYSTEM.info("Сервер ии-модели вернул ответ: " + response.length());
            if (response == null) {
                errorMessage(idChat, "Не удалось сгенерировать изображение");
                LOGGER_BOT_SYSTEM.info("Ответ от сервера не пришел.");
                return;
            }
            sendImageToUser(idChat, response);
            imageStorage.clear(idChat);
        } else {
            errorMessage(idChat, "Отправьте второе изображение.");
        }
    }

    @Override
    public String getBotUsername() {
        return nameBot;
    }
}