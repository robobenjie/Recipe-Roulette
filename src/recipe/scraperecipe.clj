(ns recipe.scraperecipe
  (:require [net.cgrand.enlive-html :as html])
  (:use [recipe.wordlabels] :reload))

(def starting-urls
  (map str '(
            http://allrecipes.com/recipe/simply-parmesan-chicken/detail.aspx
            http://allrecipes.com/recipe/cold-crawfish-dip/detail.aspx
            http://allrecipes.com/recipe/tart-and-bubbly-wedding-punch/detail.aspx
            http://allrecipes.com/recipe/blueberry-scones/detail.aspx
            http://allrecipes.com/recipe/carrot-cake-iii/detail.aspx
            http://allrecipes.com/recipe/banana-streusel-muffins/detail.aspx
            http://allrecipes.com/recipe/chicken-and-dumplings-iv/detail.aspx
            http://allrecipes.com/recipe/baby-carrots-and-brussels-sprouts-glazed-with-brown-sugar-and-pepper/detail.aspx
            http://allrecipes.com/recipe/campbells-healthy-request-green-bean-casserole/detail.aspx
            http://allrecipes.com/recipe/dandelion-salad/detail.aspx
            http://allrecipes.com/recipe/avocado-irish-cream-fudge/detail.aspx
            http://allrecipes.com/recipe/irish-lamb-stew/detail.aspx
            http://allrecipes.com/recipe/faux-jerk-chicken/detail.aspx
            http://allrecipes.com/recipe/cuban-midnight-sandwich/detail.aspx
            http://allrecipes.com/recipe/baked-shrimp-with-feta-and-tomato/detail.aspx
            http://allrecipes.com/recipe/its-chili-by-george/detail.aspx
            http://allrecipes.com/recipe/chicken-with-couscous/detail.aspx
            http://allrecipes.com/recipe/sweet-restaurant-slaw/detail.aspx
            http://allrecipes.com/recipe/cornbread-dressing-ii/detail.aspx
            http://allrecipes.com/recipe/jalapeno-blue-cheese-burgers/detail.aspx
            http://allrecipes.com/recipe/nutty-whole-wheat-bread/detail.aspx
            http://allrecipes.com/recipe/cioppino/detail.aspx
            http://allrecipes.com/recipe/western-salad/detail.aspx
            http://allrecipes.com/recipe/tuna-cheesies/detail.aspx
            http://allrecipes.com/recipe/cream-cheese-and-crab-sushi-rolls/detail.aspx
            http://allrecipes.com/recipe/banana-crumb-muffins/detail.aspx
            http://allrecipes.com/recipe/worlds-best-lasagna/detail.aspx
            http://allrecipes.com/recipe/delicious-ham-and-potato-soup/detail.aspx
            http://allrecipes.com/recipe/chicken-pot-pie-ix/detail.aspx
            http://allrecipes.com/recipe/downeast-maine-pumpkin-bread/detail.aspx
            http://allrecipes.com/recipe/clone-of-a-cinnabon/detail.aspx
            http://allrecipes.com/recipe/apple-pie-by-grandma-ople/detail.aspx
            http://allrecipes.com/recipe/moms-zucchini-bread/detail.aspx
            http://allrecipes.com/recipe/too-much-chocolate-cake/detail.aspx
            http://allrecipes.com/recipe/chicken-cordon-bleu-ii/detail.aspx
            http://allrecipes.com/recipe/annies-fruit-salsa-and-cinnamon-chips/detail.aspx
            http://allrecipes.com/recipe/guacamole/detail.aspx
            http://allrecipes.com/recipe/fluffy-pancakes-2/detail.aspx
            http://allrecipes.com/recipe/boilermaker-tailgate-chili/detail.aspx
            http://allrecipes.com/recipe/bbq-pork-for-sandwiches/detail.aspx
            http://allrecipes.com/recipe/jamies-cranberry-spinach-salad/detail.aspx
            http://allrecipes.com/recipe/maple-salmon/detail.aspx
            http://allrecipes.com/recipe/braised-skirt-steak-with-artichoke/detail.aspx

            )))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))


(def ingredient-selector [:.ingredients :li])

(defn get-ingredients [page]
  (map process-ingredient-string (map clojure.string/trim (map html/text (html/select page ingredient-selector)))))
(defn get-directions [page]
  (map clojure.string/trim (map html/text (html/select page [:.directions :li]))))
(defn get-servings [page]
  (->(first (html/select page [:.servings-form :.servings-amount])) :attrs :value read-string))
(defn get-rating [page]
  (read-string (re-find #"\d+\.?\d*" (-> (first (html/select page [:p.reviewP :img])) :attrs :title))))
(defn ingredient-string [i]
  (str  (i :amount) (if (i :units) (str " " (i :units) " of") "") " " (i :ingredient) " " (if (i :prep) (str "(" (clojure.string/join ", " (i :prep)) ")")"")))

(defn pretty-print-ingredients [ingredients]
  (doseq [i ingredients]
    (println (ingredient-string i))))

(defn pretty-print [recipe]
  (println)
  (println)
  (println "  +++++++++++  " (recipe :title) "  +++++++++++  ")
  (println)
  (println (recipe :url))
  (println "serves" (recipe :servings))
  (println (repeat (recipe :rating) '*))
  (pretty-print-ingredients (recipe :ingredients))
  (println (recipe :directions))
  (println "see also:" (recipe :similar-links))) 
  
(defn recipe-links [keyword]
    (let [page (fetch-url (str "http://allrecipes.com/search/default.aspx?qt=k&wt=" keyword))
          link-objs (html/select page [:.resultTitle :a])
	  links (map #(->> % :attrs :href (re-find #"^.*aspx")) link-objs)]
	links))

(defn scrape-link [url]
  (let [page (fetch-url url)
        link-objs (take 5 (rest (html/select page [:.leftnav-redesign :li])))
        links (map #(str "http://allrecipes.com" (-> % (get :content) first (get :attrs) (get :href))) link-objs)]
    {:url url
     :title (html/text (first (html/select page [:#itemTitle :span])))
     :ingredients (get-ingredients page)
     :similar-links links
     :directions (get-directions page)
     :rating (get-rating page)
     :servings (get-servings page)
     }
    ))


(defn scrape-n-sites [n start-links process-recipe-fn visited-links file-prefix]
  (loop [all-recipes []
         links-seen (into visited-links start-links)
         links-to-visit start-links]
    (if (or (>= (count all-recipes) n) (empty? links-to-visit))
      (do
        (spit (str file-prefix "unvisited-links.txt") (str links-to-visit))
        (spit (str file-prefix "all-links.txt") (str links-seen))
        all-recipes)
      (let [recipe (scrape-link (first links-to-visit))]
        (println (count all-recipes) "visited. " (count links-to-visit) "in the queue.     ->" (recipe :title))
        (process-recipe-fn recipe)
        (recur
         (conj all-recipes recipe)
         (into links-seen (recipe :similar-links))
         (concat (rest links-to-visit) (filter #(not (links-seen %)) (recipe :similar-links))))))))

(defn scrape-n-save [n start-links filename visited-links]
  (spit filename "[")
  (scrape-n-sites n start-links #(do (Thread/sleep 100) (spit filename % :append true)) visited-links filename)
  (spit filename "]" :append true))


(defn get-keywords [recipe]
  (-> recipe :title clojure.string/lower-case (clojure.string/replace #"[\?\!,]" "") (clojure.string/split #"\s")))


(defn load-blob [filename]
  (read-string (slurp filename)))

  


