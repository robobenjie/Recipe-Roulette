(ns recipe.manage-redis
  (:use [recipe.scraperecipe] :reload)
  (require [clojure.contrib.math]
	   [recipe.heap-sort])
  (:import  [redis.clients.jedis Jedis JedisPool]))

(defn process-recipe-to-redis [the-recipe additional-keyword]
  (let [*jedisPool* (JedisPool. "127.0.0.1" 6379)
	jedis (.getResource *jedisPool*)]
    (.select jedis 0)
    (let [keywords (into (if additional-keyword #{additional-keyword} #{}) (get-keywords the-recipe))
    	  ingredient-objs (map #(assoc % :servings (the-recipe :servings))(the-recipe :ingredients))
    	  ingredients (map :ingredient ingredient-objs)
	  url (the-recipe :url)]
      (if (.sismember jedis ":all-urls" url)
		(println "already scraped this recipe:" (the-recipe :title))
      (do
      (println "adding recipe:" (the-recipe :title))
      (.sadd jedis ":all-urls" url)
      (doseq [related-url (filter #(not (.sismember jedis ":all-urls" %)) (the-recipe :similar-links))]
	(.sadd jedis ":unscraped-urls" related-url))

      (dorun (map #(.sadd jedis ":all-keywords" %) keywords))
      (dorun (map #(.sadd jedis ":all-ingredients" %) ingredients))

      (dorun (map #(.incr jedis (str ":ingredient-count:" %)) ingredients))
      (dorun (map #(.incr jedis (str ":keyword-count:" %)) keywords))

      (.incrBy jedis ":total-keyword-count" (long (count keywords)))
      (.incrBy jedis ":total-ingredient-count" (long (count ingredients)))

      (dorun
       (for [keyword keywords
	  ingredient-obj ingredient-objs]
	  (let [ingredient (ingredient-obj :ingredient)]
		(.incr jedis (str ":keyword-ingredient-count:" keyword ":" ingredient))
		(.lpush jedis (str ":keyword-ingredient-obj:" keyword ":" ingredient) (str ingredient-obj))
		(.sadd jedis (str ":key-set:" keyword) ingredient))))
      
      (dorun (for [i1 ingredients
      	    i2 ingredients]
	(.incr jedis (str ":ingredient-ingredient-count:" i1 ":" i2))))
      (dorun (for [ingredient ingredients]
	(.incrBy jedis (str ":ingredient-ingredient-total:" ingredient) (long (count ingredients)))))))
      (.returnResource *jedisPool* jedis))))

(defn probabilities [occurances events lambda count-event-types]
  (let [occur (max lambda occurances)]
    (/ (+ occur lambda)
       (+ events (* count-event-types lambda)))))


(defn add-useless-keyword [keyword]
  (let [*jedisPool* (JedisPool. "127.0.0.1" 6379)
 	 jedis (.getResource *jedisPool*)]
    (.select jedis 0)
    (.sadd jedis ":useless-keywords" keyword)
    (.returnResource *jedisPool* jedis)))

(defn check-keyword [keyword]
  (let [*jedisPool* (JedisPool. "127.0.0.1" 6379)
	jedis (.getResource *jedisPool*)]
      (.select jedis 0)
      (let [retval
         (cond
	 (.sismember jedis ":all-keywords" keyword) :good
	 (.sismember jedis ":useless-keywords" keyword) :bad
	 :default :search)]
      
      (.returnResource *jedisPool* jedis)
      retval)))
(defn search-and-add-keyword [keyword]
    (let [urls (recipe-links keyword)]
	(if (empty? urls)
	    (add-useless-keyword keyword)
	    (doseq [r (map scrape-link urls)]
		(process-recipe-to-redis r keyword)))))

(defn construct-recipe [keywords]
  (let [*jedisPool* (JedisPool. "127.0.0.1" 6379)
	jedis (.getResource *jedisPool*)]
    (.select jedis 0)
    (let[ingredients (filter #(< 2 (count %)) (apply vector (reduce #(into %1 %2) #{}
					   (map #(.smembers jedis (str ":key-set:" %)) keywords))))
	 num-ingredient-types (.scard jedis ":all-ingredients")
	 num-keyword-types (.scard jedis ":all-keywords")
	 num-ingredients-ever (read-string (.get jedis ":total-ingredient-count"))
	 num-keywords-ever (read-string (.get jedis ":total-keyword-count"))
	 ingredient-count (memoize (fn [i] (read-string (.get jedis (str ":ingredient-count:" i)))))
	 P_i (fn [i]
	       (probabilities (ingredient-count i)
			      num-ingredients-ever
			      105.0
			      num-ingredient-types))
	 P_k (fn [k]
	       (probabilities (read-string (.get jedis (str ":keyword-count:" k)))
			      num-keywords-ever
			      5.0
			      num-keyword-types))
	 
	 prob-of-all-keywords (reduce * (map P_k keywords))

	 P_k|i (memoize (fn [k i]
		 (probabilities (read-string (or (.get jedis (str ":keyword-ingredient-count:" k ":" i)) "0"))
				(ingredient-count i)
				5.0
				num-ingredients-ever)))
	 P_i|ks (memoize (fn [i]
		  (*
		   (reduce * (map #(P_k|i % i) keywords))
		   (/ (+ 0.0002 (P_i i)) (+ 0.0 (P_i i)))
		   (/ 1 prob-of-all-keywords))))
	 comp-fn (memoize (comparator #(> (P_i|ks %1) (P_i|ks %2))))
	 n-best-fit (fn [n ks]
		      (take n (recipe.heap-sort/heap-sort 
			        comp-fn
				ingredients)))
	 probabalistic-ingredients (take 15 (filter #(< (rand) (* 1500 (P_i|ks %))) ingredients))
	 keyword-match-ingredients (into #{} (filter #(not (nil? %)) (map 
			(fn [keyword] (first 
				(recipe.heap-sort/heap-sort comp-fn 
				    (filter 
					#(re-find (re-pattern keyword) %) 
					ingredients)))) 
			keywords)))

	 ret-ingredients (into #{} (n-best-fit 9 keywords))
	 ret-recipe (map ingredient-string
			 (map (fn [ingredient]
				(let [keyword (first (reverse (sort-by
							       #(P_k|i % ingredient)
							       (filter
								#(.sismember jedis (str ":key-set:" %) ingredient)
								keywords))))]
				  (read-string
				   (.lindex jedis  (str ":keyword-ingredient-obj:" keyword ":" ingredient) 0))))
			      (into keyword-match-ingredients ret-ingredients)))
	 ]
      (.returnResource *jedisPool* jedis)
     ret-recipe)))
  
					 




(defn make-jedis-test []
      (def jedisPool (JedisPool. "127.0.0.1" 6379))
      (def jedis (.getResource jedisPool))
      (.select jedis 0))

(defn make-args [recipe]
      (def keywords (get-keywords recipe))
      (def ingredient-objs (map #(assoc % :servings (recipe :servings))(recipe :ingredients)))
      (def ingredients (map :ingredient ingredient-objs)))

 (defmacro rs [& kw]
    `(doseq [line# (construct-recipe (map str '~kw))]
       (println line#)))
 