package com.toktot.domain.localfood;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum LocalFoodType {

    DOMBE_MEAT("돔베고기", "dombe_meat_icon",
            Arrays.asList(
                    "돔베고기", "돔베", "돔베기", "돔베육", "돔베삽겹", "돔베삼겹살",
                    "돔배고기", "도메고기", "돔비고기", "돔베기고기",
                    "뽀글이", "뽀글", "또메고기", "돔베돼지", "돔베삼겹",
                    "돔베기", "돔베육삽", "도매고기", "돔바고기", "돔배기"
            )),

    MEAT_NOODLE_SOUP("고기국수", "meat_noodle_icon",
            Arrays.asList(
                    "고기국수", "고깃국수", "제주국수", "고기국시", "국수고기", "고기면",
                    "고국수", "제주고기국수", "고기궉수", "궉수", "국시",
                    "고깃국시", "고기국시", "궉시", "궉수국"
            )),

    SEA_URCHIN_SEAWEED_SOUP("성게미역국", "sea_urchin_seaweed_icon",
            Arrays.asList(
                    "성게미역국", "성게미역", "성게국", "미역성게국", "성게탕", "성게국밥",
                    "성게미역탕", "성게국수", "성게죽", "성게맑은국",
                    "섬게미역국", "섬게국", "성게국시", "섬게탕"
            )),

    BRACKEN_HANGOVER_SOUP("고사리 해장국", "bracken_hangover_icon",
            Arrays.asList(
                    "고사리해장국", "고사리국", "해장국", "고사리탕", "고사리국밥",
                    "고사리해장", "고사리육개장", "제주해장국", "고사리맑은탕",
                    "고사리헹궉", "고사리궉", "고사리국시"
            )),

    GRILLED_RED_TILEFISH("옥돔구이", "red_tilefish_icon",
            Arrays.asList(
                    "옥돔구이", "옥돔", "제주옥돔", "옥돔회", "옥돔조림", "옥돔구워", "옥돔구운거",
                    "옥도미", "옥돔이", "옥도미구이", "옥돔구윽"
            )),

    GRILLED_CUTLASSFISH("갈치구이", "cutlassfish_icon",
            Arrays.asList(
                    "갈치구이", "갈치조림", "갈치", "제주갈치", "은갈치", "갈치튀김",
                    "갈치회", "갈치찜", "갈치맛집", "갈치국", "갈치국밥",
                    "갈취", "갤치", "가리치", "갤취"
            )),

    RAW_FISH_MULHOE("회/물회", "raw_fish_icon",
            Arrays.asList(
                    "물회", "회", "생선회", "횟감", "활어회", "모듬회",
                    "광어회", "우럭회", "제주물회", "활어물회", "횟집", "사시미",
                    "회이", "뮬회", "무레", "횟이"
            )),

    BING_RICE_CAKE("빙떡", "bing_icon",
            Arrays.asList(
                    "빙떡", "빙떡이", "밀전병", "메밀떡", "빙전", "빙떡전", "빙떡메밀",
                    "빙떡이", "빙득", "빙닥", "빙떡애"
            )),

    OMEGI_RICE_CAKE("오메기떡", "omegi_icon",
            Arrays.asList(
                    "오메기떡", "오메기", "제주떡", "오메기밥", "찰떡오메기",
                    "오메기한과", "오메기디저트", "흑임자오메기",
                    "오매기", "오메귀", "오멩이떡", "오매기떡"
            )),

    BLACKPORK_BBQ("흑돼지구이", "blackpork_bbq_icon",
            Arrays.asList(
                    "흑돼지구이", "흑돼지", "흑돼지고기", "제주흑돼지", "흑돼지삼겹살",
                    "흑돼지오겹살", "제주돼지", "흑돼지석쇠", "흑돼지바베큐",
                    "흑돼지맛집", "흑돼지불판", "흑돼지숯불",
                    "흑돼지기", "흑되지", "검은돼지", "검둥돼지", "흑돼지기이"
            )),

    ABALONE_KIMBAP("전복김밥", "abalone_kimbap_icon",
            Arrays.asList(
                    "전복김밥", "전복", "전복요리", "전복밥", "전복회", "전복죽",
                    "전복비빔밥", "전복맛집", "전복해물밥",
                    "졘복", "전복이", "졘복김밥", "전복밥이"
            )),

    ;

    private final String displayName;
    private final String iconName;
    private final List<String> keywords;

    public static Optional<LocalFoodType> findByMenuName(String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedMenuName = normalizeMenuName(menuName);

        return Arrays.stream(values())
                .filter(type -> type.keywords.stream()
                        .map(LocalFoodType::normalizeMenuName)
                        .anyMatch(normalizedMenuName::contains))
                .findFirst();
    }

    private static String normalizeMenuName(String menuName) {
        return menuName.toLowerCase()
                .replaceAll("[^가-힣a-z0-9]", "")
                .trim();
    }

    public List<String> getKeywords() {
        return List.copyOf(keywords);
    }
}
