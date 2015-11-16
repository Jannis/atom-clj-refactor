(ns atom-clj-refactor.lang
  (:require [clojure.string :as str]
            [cljs.reader :refer [read-string]]
            [atom-clj-refactor.editor :as e]))

(defn read-meta [tokens]
  (let [s (->> tokens (map e/value) str/join str/trim)]
    (if (empty? s)
      {}
      (do
        (assert (= "^" (first s)))
        (let [m (read-string (subs s 1))]
          (if (map? m) m {m true}))))))

(defn write-meta [m]
  (if (empty? m)
    ""
    (str "^" (pr-str (if (= 1 (count m))
                       (if (true? (first (vals m)))
                         (first (keys m))
                         m)
                       m)))))
