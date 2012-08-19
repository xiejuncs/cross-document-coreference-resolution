import java.util.List;		
import java.util.ArrayList;

public class example {

	private static List<Integer> list;

	public static void main(String[] args) {
		list = new ArrayList<Integer>();
		list.add(1);
		list.add(12);
		list.add(5);
		list.add(26);
		list.add(7);
		list.add(14);
		list.add(3);
		list.add(7);
		list.add(2);		
		quicksort( 0, list.size() - 1 );
			System.out.println(list);	
	}

	public static int partition(int left, int right) {
		int i = left, j = right + 1;
		int tmp;
		int pivot = list.get(left);


		while (true) {
			while (list.get(++i) < pivot) {
				if (i == right) break;
			}		
			while (list.get(--j) > pivot) {
				if (j == left) break;
			}
			if (i>= j) break;
				tmp = list.get(i);
				list.set(i, list.get(j));
				list.set(j, tmp);
		}
		tmp = list.get(left);
		list.set(left, list.get(j));
		list.set(j, tmp);
		return j;
	}

	public static void quicksort(int left, int right) {
		if (right <= left) return;
		int index = partition( left, right);
		quicksort( left, index - 1);	
		quicksort( index + 1, right);
		
	}
}
