

(defn extract-all-ingredients [blob]
  (into #{} (map #(% :ingredient) (apply concat (map #(-> % :ingredients) blob)))))

(defn extract-all-urls [blob]
  (into #{} (map :url blob)))

(defn make-probabilities [long-list lambda numtypes]
  (let [denominator (+ (* numtypes lambda) (count long-list))
	start-val (/ lambda denominator)
	frac-inc #(+ % (/ 1 denominator))
	big-map 
	(loop [words long-list prob-map {}]
	  (if (empty? words)
	    prob-map
	    (recur
	     (rest words)
	     (assoc prob-map
	       (first words)
	       (if-let [value (prob-map (first words))]
		 (frac-inc value)
		 (frac-inc 0.0))))))]
    (fn [word] (or (big-map word) start-val))))

(defn make-keyword-probabilities [blob lambda]
  (let [keywords (apply concat (map get-keywords blob))]
    (make-probabilities keywords lambda (count (set keywords)))))

(defn make-ingredient-probabilities[blob lambda]
  "returns a function that gives the lambda smoothed probability of a
   given ingredient. I assume that I have about 80% of all possible
   ingredients"
  (let [all-ingredients (map #(% :ingredient) (apply concat (map #(-> % :ingredients) blob)))]
    (make-probabilities all-ingredients lambda (* 1.2 (count (set all-ingredients)))))) 

(defn inc-ingredient [keyword-map ingredient lambda]
  (let [name (ingredient :ingredient)]
    (if (keyword-map name)
      (assoc-in keyword-map [name :prob]
		(inc (get-in keyword-map [name :prob])))
      (assoc keyword-map name (assoc ingredient :prob (+ 1 lambda))))))

(defn update-keyword [keymap keyword value-list lambda]
  (assoc keymap keyword
	 (reduce #(inc-ingredient %1 %2 lambda) (or (keymap keyword) {}) value-list)))

(defn normalize-keyword-probs [keyword-map lambda num-possible-values]
  (let [denominator ( +
		      (reduce + (map (fn [[name obj]] (obj :prob)) keyword-map))
		      (* lambda num-possible-values))]
    (reduce (fn [m k] (update-in m [k :prob] #(/ % denominator))) keyword-map (keys keyword-map)))) 
		   

(defn make-keyword-ingredient-probabilities [blob lambda]
  (let [un-normalized
    (loop [unprocessed blob
	   retval {}]
      (if (empty? unprocessed) retval
	  (recur
	   (rest unprocessed)
	   (let [keywords (get-keywords (first unprocessed))
		 ingredients ((first unprocessed) :ingredients)]
	     (reduce #(update-keyword %1 %2 ingredients lambda) retval keywords)))))]
    (reduce (fn [m k] (update-in m [k] #(normalize-keyword-probs %1 lambda (count (extract-all-ingredients blob))))) un-normalized (keys un-normalized))))


(defn conditional-prob-ingredient-given-keyword [probability-map lambda total-ingredients]
  (fn [ingredient keyword]
    (if-let [prob (get-in probability-map [keyword ingredient :prob])]
      prob
      (/ lambda total-ingredients))))

(defn conditional-prob-keyword-given-ingredient [P_i|k P_i P_k]
  (fn [i k]
    (* (P_i|k i k) (P_i i) (/ 1.0 (P_k k)))))

(defn joint-prob-fn-i-given-ks [P_k|i P_i P_k]
  (fn [ingredient keywords]
    (*
     (reduce * (map #(P_k|i ingredient %) keywords))
     (+ (/ 1 (P_i ingredient)) 1E-25)
     (reduce * (map P_k keywords)))))


(defn process-blob [blob lambda lambda-ingredient]
  (def keyword-map (make-keyword-ingredient-probabilities blob lambda))
  (def P_i (make-ingredient-probabilities blob lambda-ingredient))
  (def P_k (make-keyword-probabilities blob lambda))
  (def P_i|k (conditional-prob-ingredient-given-keyword keyword-map lambda (count (extract-all-ingredients blob))))
  (def P_k|i (memoize (conditional-prob-keyword-given-ingredient P_i|k P_i P_k)))
  (def P_i|klist (joint-prob-fn-i-given-ks P_k|i P_i P_k))
  (defn get-ingredient-set [keyword-list]
    (into #{} (apply concat (map #(-> % keyword-map keys) keyword-list))))
  (defn n-best-fit-ingredients [n keywords]
    (take n (reverse (sort-by #(P_i|klist %1 keywords) (get-ingredient-set keywords)))))
  (defn n-most-corrolated [n keyword]
    (take n (reverse (sort-by #(/ (P_i|k % keyword) (P_i %)) (get-ingredient-set [keyword])))))
  (defn recipe-ingredients [keywords]
    (into #{} (concat (n-best-fit-ingredients 10 keywords) (apply concat (map #(n-best-fit-ingredients 6 [%]) keywords)))))
  (defn recipe [keywords]
    (let [i-list (filter #(> (count %) 1) (recipe-ingredients keywords))
          get-i-obj (fn [ingredient keyword] (get-in keyword-map [keyword ingredient]))]
      (map ingredient-string
           (map (fn [ingredient] (get-i-obj ingredient (first (reverse (sort-by #(if (get-i-obj ingredient %) (P_i|k ingredient %) 0) keywords)))))
                i-list))))
      
  (defmacro r [& kw]
    `(doseq [line# (recipe (map str '~kw))]
       (println line#)))
 )
