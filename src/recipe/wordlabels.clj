(ns recipe.wordlabels
  (:require [clojure.string]))

(def boring-keywords 
 (into #{} (map str '(
		with
		of
		the
		and
		or
               and
               of
               to
               in
               at
	       if
	       are
))))

(def measurement-units
  (into #{} (map str '(
    cup
    ounce
    tablespoon
    pinch
    clove
    pound
    jar
    teaspoon
    gram
    pint
    quart
    gallon
    head
    package
    container
    can
    half
    bunch
    slice
    halves
    oz
    tbs
    tsp
    cp
    pt
    qt
    lb
    stick
    cube
    stalks
    sprig
    bottle
    dash
    packet
    box
    sheet
    tub
    ))))
(def to-cups
  (let [conversion
	(apply hash-map
	 '(cup 1
    ounce 0.125
    tablespoon 1/16
    pinch 0.001
    clove 0.05
    pound 2
    jar 1
    teaspoon 0.02
    gram 0.004
    pint 2
    quart 4
    gallon 16
    head 3
    package 2
    container 2
    can 1
    half 1
    bunch 2
    slice 0.3
    halves 1
    oz 0.125
    tbs 1/16
    tsp 0.02
    cp 1
    pt 2
    qt 4
    lb 2
    stick 1
    cube 1
    stalks 1
    sprig 0.25
    bottle 2
    dash 0.03
    packet 0.02
    box 3
    sheet 2
    tub 6))]
    (fn [{:keys [units, amount]}]
      (println units amount)
      (if (nil? units)
	amount
	(let [unit (clojure.string/replace units #"s$" "")]
	  (println "conversion is" (conversion (symbol unit)) "of type" (type (conversion (symbol unit))))
	  (if-let [c (conversion (symbol unit))]
	    (* amount c)
	    amount))))))


(def prep-styles
  (into #{}
        (map str '(
		   diced
		   lean
               minced
               peeled
               deveined
               grated
               chopped
               ground
               sliced
               fresh
               frozen
               thawed
               large
               small
               medium
               shredded
               canned
               uncooked
               prepared
               "long grain"
               long-grain
               longgrain
               cooked
               undiluted
               processed
               cubed
               drained
               cut
               unsalted
               pieces
               cleaned
               debearded
               filleted
               fillets
	       fillet
               "bite size"
               "bite sized"
               beaten
               bits
               softened
               toasted
               hulled
               blanched
               slivered
               strips
               skinless
               boneless
               finger-sized
               warm
               cool
               cold
	       chilled
	       crumbled
               "room temperature"
               hot
               dried
               pitted
               seeded
               cooked
               grilled
               washed
               trimmed
               leaves
               "stew meat"
               juiced
               crushed
               packed
               melted
	       chunky
	       rounds
               liter
	       "for brushing"
               "all-purpose"
               mashed
               halved
               thin
               scored
	       cored
               torn
	       quartered
	       shucked
	       tiny
	       thick
	       tenderized
	       pounded
	       "low fat"
	       "low sodium"
	       "low-sodium"
	       "reduced-sodium"
	       "reduced-fat"
	       "fat-free"
	       "fat free"
	       rinsed
	       "for topping"
	       whole
	       rinsed
	       "bite-size"
	       raw
	       chilled
	       ripe
	       soft
	       dry
	       fireroasted
	       "fire roasted"
	       rinsed
	       sifted
               ))))
(def adjective-re
  #"\b[a-zA-Z]+ly\b")

(def ignore-words
  (into #{}
        (map str '(
               or
               and
	       pure
	       generous
	       kraft
               of
               to
               taste
               adjust
               personal
               fluid
               degrees
	       reduced
	       sodium
	       very
	       plus
               f
               c
               more
               inch
               into
               divided
               premium
               in
               at
	       healthy
	       if
	       are
               ))))
(defn set-to-re [word_set]
  (re-pattern (clojure.string/join "|" (map #(str "\\b" % "\\b") (seq word_set)))))

(def units-re-with-s
  (re-pattern (clojure.string/join "|" (map #(str "\\b" % "s?\\b") (seq measurement-units)))))
(def units-re-without-s
  (re-pattern (clojure.string/join "|" (map #(str "\\b" % "(?:s)?\\b") (seq measurement-units)))))
(def amount-re
  #"^\d+[./]?\d*\s?\d*/?\d*")
(def prep-re
  (set-to-re prep-styles))
(defn get-units [s]
  (re-find units-re-without-s (clojure.string/replace s #"\(.+\)" "")))
(defn get-amount [s]
  (reduce + (map read-string (#(or % "0") (clojure.string/split (or (re-find amount-re s) "0") #"\s")))))
(defn get-prep [s]
  (re-seq prep-re s))
(defn get-ingredient [s]
  (-> s
      (clojure.string/replace amount-re "")
      (clojure.string/replace units-re-with-s "")
      (clojure.string/replace #"\(.+\)" "") ;remove parentheticals
      (clojure.string/replace #"\bor .*$" "") ; only take the first option in eg "butter or margerine"
      (clojure.string/replace #"\bfor .*$" "") ; don't care what it is for
      (clojure.string/replace #"\bwith .*$" "") ; only take the first option in eg "tomatos with olive oil"
      (clojure.string/replace #"\s[a-zA-Z']*[Â®\?]+" "") ;remove copywritten words eg "French's® french fried onions"
      (clojure.string/replace (set-to-re ignore-words) "")
      (clojure.string/replace prep-re "")
      (clojure.string/replace adjective-re "")
      (clojure.string/replace #"[\d\";():,.\/?\\]" "") ;remove funny symbols
      (clojure.string/replace #"\s+" " ") ;remove redundant white space
      (clojure.string/trim )
      (clojure.string/lower-case)
      ))
      
(defn process-ingredient-string [s]
  {:amount (get-amount s)
   :units (get-units s)
   :ingredient (get-ingredient s)
   :prep (get-prep s)})


