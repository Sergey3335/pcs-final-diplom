import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {
    private Map<String, List<PageEntry>> map = new HashMap<>();
    public BooleanSearchEngine(File pdfsDir) throws IOException {

        for (File pdf : pdfsDir.listFiles()) {
            var doc = new PdfDocument(new PdfReader(pdf));
            int NumberOfPages = doc.getNumberOfPages();
            for (int i = 1;i <= NumberOfPages; i++) {
                var page = doc.getPage(i);
                var text = PdfTextExtractor.getTextFromPage(page);
                var words = text.split("\\P{IsAlphabetic}+");

                Map<String, Integer> freqs = new HashMap<>(); // мапа, где ключом будет слово, а значением - частота
                for (var word : words) { // перебираем слова
                    if ((word.isEmpty())) {
                        continue;
                    }
                    word = word.toLowerCase();
                    freqs.put(word, freqs.getOrDefault(word, 0) + 1);
                }
                for (Map.Entry<String, Integer> entry : freqs.entrySet()) {
                    var word = entry.getKey();
                    var count = entry.getValue();
                    PageEntry pageEntry = new PageEntry(pdf.getName(), i, count);

                    if (map.containsKey(word)) {
                        map.get(word).add(pageEntry);
                    } else {
                        map.computeIfAbsent(word, w -> new ArrayList<>()).add(pageEntry);
                    }
                }
            }
        }
    }

    @Override
    public List<PageEntry> search(String words) {

        String[] request = words.toLowerCase().split("\\P{IsAlphabetic}+");
        List<PageEntry> tempList = new ArrayList<>();
        List<PageEntry> result = new ArrayList<>();

        for (String newWord : request) {
            if (map.get(newWord) != null) {
                tempList.addAll(map.get(newWord));
            }
        }
        Map<String, Map<Integer, Integer>> numberAndQuantity = new HashMap<>();
        for (PageEntry pageEntry : tempList) {
            numberAndQuantity.computeIfAbsent(pageEntry.getPdfName(), key -> new HashMap<>())
                    .merge(pageEntry.getPage(), pageEntry.getCount(), Integer::sum);
        }

        numberAndQuantity.forEach((key, value) -> {
            for (var tempPage : value.entrySet()) {
                result.add(new PageEntry(key, tempPage.getKey(), tempPage.getValue()));
            }
        });
        Collections.sort(result);
        return result;
    }
}
