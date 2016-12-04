package ocr.lexicon;

import utils.EditDistance;

/**
 * Created by itjamal on 12/4/2016.
 */
public class LexClass1 {
    String[] Shops = {"SAFA", "SPAR", "BAZARSTORE", "BOLMART", "FAVORIT"};
    String[] Voens = {"1403428411", "1001994141", "2000102651-17001", "6969696", "121212"};

    EditDistance ed;

    public LexClass1() {
        ed = new EditDistance();
    }

    public String defineDocAuthor(String text) {
        double minDist = 69;
        String winner = "";

        for (String shop : Shops) {
            double dist = ed.findDistance(shop, text) * 1.0 / shop.length();
            if ((dist <= 0.25) && (dist < minDist)) {
                minDist = dist;
                winner = shop;
            }
        }

        return winner;
    }

    public String defineDocAuthorbyVOEN(String text) {
        double minDist = 69;
        String winner = "";
        int idx = 0;

        for (String voen : Voens) {
            double dist = ed.findDistance(voen, text) * 1.0 / voen.length();
            if ((dist <= 0.25) && (dist < minDist)) {
                minDist = dist;
                winner = Shops[idx];
            }
            idx++;
        }

        return winner;
    }

    public boolean isVOEN(String str) {
        int dist = ed.findDistance("VOEN", str);

        return dist < 2;
    }

    public boolean isDate(String str) {
        int dist = Math.min(ed.findDistance("TARIH", str), ed.findDistance("TARIX", str));

        return dist < 2;
    }

    public boolean isTime(String str) {
        int dist = ed.findDistance("SAAT", str);

        return dist < 2;
    }

}
