import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class zip {
    static <A, B> List<Pair<A, B>> zip(List<A> list1, List<B> list2, A default1, B default2) {
        Iterator<A> it1 = list1.iterator();
        Iterator<B> it2 = list2.iterator();
        List<Pair<A, B>> result = new ArrayList<>();
        while (it1.hasNext() || it2.hasNext()) {
            A aa = default1;
            B bb = default2;
            if (it1.hasNext()) {
                aa = it1.next();
            }
            if (it2.hasNext()) {
                bb = it2.next();
            }
            result.add(new Pair<>(aa, bb));
        }
        return result;
    }
}
