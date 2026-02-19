package klikr.diskview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.Rectangle2D;

/**
 * Squarified treemap layout algorithm based on Bruls, Huizing, van Wijk (2000).
 *
 * The key idea: we lay out items row by row along the shorter side of the remaining
 * rectangle. Each row is a strip whose thickness is determined by the total area of
 * the items in that row. Items within a row are stacked along the longer side.
 *
 * We greedily add items to a row as long as the worst aspect ratio improves.
 */
//**********************************************************
public class Treemap_layout
//**********************************************************
{

    //**********************************************************
    public static class Layout_result
    //**********************************************************
    {
        public final File_node node;
        public final Rectangle2D bounds;
        public final List<Layout_result> children;

        //**********************************************************
        public Layout_result(File_node node, Rectangle2D bounds, List<Layout_result> children)
        //**********************************************************
        {
            this.node = node;
            this.bounds = bounds;
            this.children = children;
        }
    }

    //**********************************************************
    public static Layout_result calculateAt(File_node root, double x, double y, double width, double height)
    //**********************************************************
    {
        Rectangle2D bounds = new Rectangle2D(x, y, width, height);
        
        List<File_node> children = new ArrayList<>(root.get_children());
        children.removeIf(n -> n.get_size() <= 0);
        
        List<Layout_result> childResults = new ArrayList<>();
        
        if (!children.isEmpty() && width > 0 && height > 0) {
            // Sort by size descending (required by squarify)
            children.sort(Comparator.comparingLong(File_node::get_size).reversed());
            
            // Limit to top 200 children for performance
            int maxChildren = 200;
            if (children.size() > maxChildren) {
                children = new ArrayList<>(children.subList(0, maxChildren));
            }
            
            // Total size of the children we're laying out
            double totalSize = children.stream().mapToLong(File_node::get_size).sum();
            
            // Total area available
            double totalArea = width * height;

            squarify(children, x, y, width, height, totalSize, totalArea, childResults);
        }
        
        return new Layout_result(root, bounds, childResults);
    }

    /**
     * Classic squarify: iteratively consume items, building rows along the shorter side.
     *
     * For each row we pick items greedily: keep adding items as long as the worst
     * aspect ratio of the row improves. Once it worsens, we commit the row, slice
     * off the used strip, and continue with the remaining rectangle.
     */
    //**********************************************************
    private static void squarify(List<File_node> items, double x, double y, double w, double h,
                                 double total_size, double total_area, List<Layout_result> results)
    //**********************************************************
    {
        int index = 0;
        int n = items.size();

        while (index < n && w > 0 && h > 0) {
            // One item left → give it the entire remaining rectangle
            if (n - index == 1) {
                results.add(new Layout_result(items.get(index), new Rectangle2D(x, y, w, h), null));
                break;
            }
            
            // The "short side" of the remaining rectangle is what we lay rows across.
            double short_side = Math.min(w, h);

            // Greedily find how many items go into this row.
            // We add items one-by-one and stop when the worst aspect ratio starts increasing.
            double row_size = 0;          // sum of sizes of items in the current row
            double previous_worst = Double.MAX_VALUE;
            int row_end = index;            // exclusive end of row

            for (int i = index; i < n; i++)
            {
                double candidate_size = row_size + items.get(i).get_size();
                // The strip thickness for this row (in the "short side" direction)
                // is proportional to the row's fraction of total area.
                double strip_thickness = (candidate_size / total_size) * (total_area / short_side);
                // But we must also guard: stripThickness = area / shortSide could exceed
                // the perpendicular dimension. Clamp to reality:
                // Actually, with correct math: strip area = total_area * (candidateSize/total_size)
                // strip is shortSide × stripThickness → stripThickness = stripArea / shortSide
                // This is always ≤ the long side by construction (it only exceeds if total_size is wrong).

                double worst = worst_aspect_ratio(items, index, i + 1, candidate_size, short_side, strip_thickness);

                if (worst <= previous_worst) {
                    previous_worst = worst;
                    row_size = candidate_size;
                    row_end = i + 1;
                } else {
                    // Adding this item made things worse → stop, don't include it
                    break;
                }
            }

            // Commit the row [idx, rowEnd)
            // Strip thickness
            double stripThickness = (row_size / total_size) * (total_area / short_side);

            if (w >= h) {
                // Short side is h → row is a vertical strip on the left, width = stripThickness
                layout_strip(items, index, row_end, row_size, x, y, stripThickness, h, true, results);
                x += stripThickness;
                w -= stripThickness;
            } else {
                // Short side is w → row is a horizontal strip on top, height = stripThickness
                layout_strip(items, index, row_end, row_size, x, y, w, stripThickness, false, results);
                y += stripThickness;
                h -= stripThickness;
            }
            
            total_size -= row_size;
            total_area = w * h;  // recalculate remaining area to avoid drift
            index = row_end;
        }
    }
    
    /**
     * Compute the worst (maximum) aspect ratio among items [start, end) if they were
     * laid out in a strip of dimensions shortSide × stripThickness.
     *
     * Items are stacked along shortSide (each gets a slice of shortSide proportional
     * to its size), while stripThickness is fixed for all items.
     */
    //**********************************************************
    private static double worst_aspect_ratio(List<File_node> items, int start, int end,
                                             double row_size, double short_side, double strip_thickness)
    //**********************************************************
    {
        if (strip_thickness <= 0 || short_side <= 0 || row_size <= 0) return Double.MAX_VALUE;

        double worst = 0;
        for (int i = start; i < end; i++) {
            double item_size = items.get(i).get_size();
            // Item's length along shortSide
            double item_length = short_side * (item_size / row_size);
            // Item's other dimension is stripThickness (constant for all items in the row)

            if (item_length > 0 && strip_thickness > 0) {
                double ratio = Math.max(item_length / strip_thickness, strip_thickness / item_length);
                worst = Math.max(worst, ratio);
            }
        }
        return worst;
    }

    /**
     * Lay out items [start, end) in a strip.
     *
     * @param vertical if true, the strip is a vertical column (fixed width = stripW, items stacked vertically).
     *                 if false, the strip is a horizontal row (fixed height = stripH, items stacked horizontally).
     */
    //**********************************************************
    private static void layout_strip(List<File_node> items, int start, int end, double rowSize,
                                     double x, double y, double strip_width, double strip_height,
                                     boolean vertical, List<Layout_result> results)
    //**********************************************************
    {
        if (start >= end || strip_width <= 0 || strip_height <= 0 || rowSize <= 0) return;

        double pos = vertical ? y : x;
        double total_length = vertical ? strip_height : strip_width;

        for (int i = start; i < end; i++) {
            double fraction = items.get(i).get_size() / rowSize;
            double item_length;
            
            if (i == end - 1) {
                // Last item takes all remaining space to avoid floating-point gaps
                item_length = (vertical ? (y + strip_height) : (x + strip_width)) - pos;
            } else {
                item_length = total_length * fraction;
            }
            
            Rectangle2D item_bounds;
            if (vertical) {
                // Items stacked vertically within a strip of fixed width
                item_bounds = new Rectangle2D(x, pos, strip_width, item_length);
            } else {
                // Items stacked horizontally within a strip of fixed height
                item_bounds = new Rectangle2D(pos, y, item_length, strip_height);
            }
            
            results.add(new Layout_result(items.get(i), item_bounds, null));
            pos += item_length;
        }
    }
}
