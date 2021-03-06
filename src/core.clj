(ns macbook-kb-cover
  (:require [scad-clj.model :refer :all]
            [scad-clj.scad :refer :all]))

;;;;;; Constants
(def scad-fn 50)
(def tolerance 0.5)
(def print-res 0.4)

(def u-y-d
  "Cap y-size. Shared by all caps"
  16.6)

(def speaker-x-d 14.0)
; (def speaker-x-d 36.2)
(def speaker-y-d 107.1)
(def keyboard-x-d 273.5)
(def keyboard-y-d 107.2)
(def trackpad-x-d 135.5)
(def trackpad-y-d 30.0)
; (def real-speaker-x-d (- 298.0 273.5))


(def real-gap 2)
;(def gap-offset (- real-gap (- real-gap wanted-gap)))
(def gap-offset (* print-res 1))

(def gap-d
  "Gap between each each for both axes"
  { :x (- real-gap gap-offset) :y (- real-gap gap-offset) })

(def left-d
  "Size of the left half of the cover"
  { :x (/ (+ (* 2 speaker-x-d) 273.5) 2) :y 107.2 })

(def right-d
  "Size of the right of the cover"
  { :x (left-d :x) :y 107.2 })

(def actual-cap-height 0.65)
(def cap-height 1.2)
(def cover-additional-z 1)

;; Shapes
(def over
  (cube (left-d :x)
        (+ (* (+ gap-offset u-y-d) 5) (* (gap-d :y) 4))
        (+ cover-additional-z cap-height)
        :center false))

(def speaker-holes
  (let [rs 1.5
        hole (->> (cylinder rs 10)
                   (translate [rs rs 0]))

        factor 2
        holes (translate [(* factor rs) (* factor rs) 0]
                         (for [ii (range 14)]
                           (for [i (range 2)]
                             (translate [(* i (* 4 rs)) (* rs 4 ii) 0]
                                        hole))))]
    (-# holes)))

(def speaker
  (cube speaker-x-d
        speaker-y-d
        cap-height
        :center false))

(def right-speaker-base
  (cube (+ 20 speaker-x-d)
        speaker-y-d
        cap-height
        :center false))

(def keyboard-center [(- (left-d :x) speaker-x-d) 0 0])

(def thumb-cluster-half (translate [0 0 cap-height] (cube (/ trackpad-x-d 2) trackpad-y-d cover-additional-z :center false)))

(def right-thumb-cluster 
  ; grr offset
  (translate [(+ (keyboard-center 0) 1.0) 0 0]
    (mirror [0 1 0] thumb-cluster-half)))

(def left-thumb-cluster 
  (translate keyboard-center 
    (mirror [1 0 0] (mirror [0 1 0] thumb-cluster-half))))


(def left-speaker
  (mirror [1 0 0] speaker))

(def right-speaker
  (let [nb-top-key-in-u (+ 14.5)
        nb-key-for-gaps 13
        original-gap-d (gap-d :x)
        u1-d 17
        magicnr 6.5]
    (translate [(+ magicnr (* nb-top-key-in-u u1-d) (* nb-key-for-gaps original-gap-d)) 0 0] right-speaker-base)))

(def keyboard
  (let [merge-vectors (comp vec flatten conj) 

        ;; Returns x/y sizes for a given cap type (eg. 1u, 2u)
        get-cap-dim (fn [name axis]
                      (+ gap-offset ((case name
                          ;; Basic key
                          :u1 {:x 17 :y u-y-d}

                          ;; Cmd/Lshift
                          :u1point25 { :x 21.76 :y u-y-d }

                          ;; shitty enter bottom
                          :u0point25 {:x 12.3 :y (+ (gap-d :y) (* u-y-d 2))}

                          ;; Capslock
                          :u1point75 { :x 31.2 :y u-y-d }

                          ;; Backspace/tab
                          :u1point5 { :x 26.5 :y u-y-d }

                          :space { :x 93.08 :y u-y-d }

                          ;; Rshift
                          :u2 { :x 40.7 :y u-y-d }) axis)))

        ;; Defines the rows of keys by their dimensions, first item is the bottom row
        kb-map [(merge-vectors [] (take 3 (repeat :u1)) :u1point25 :space :u1point25 (take 4 (repeat :u1)))
                (merge-vectors [] :u2 (take 10 (repeat :u1)) :u2)
                (merge-vectors [] :u1point75 (take 11 (repeat :u1)) :u1point75)
                (merge-vectors [] :u1point5 (take 13 (repeat :u1)))
                (merge-vectors [] (take 13 (repeat :u1)) :u1point5)]

        ;; Places all the key for a row given the data
        ;; and the y offset at which the row should be positioned.
        place-row (fn [row-data y-offset]
                    (loop [row_     row-data
                           x-offset 0  ;; Accumulator to offset the placement of the next key
                           row-caps [] ;; Accumulator to return all the keys in a row 
                           ]
                      (if (empty? row_)
                        row-caps ;; Exit loop if there is no more key to process
                        (let [cap-type   (first row_)
                              cap-x      (get-cap-dim cap-type :x)
                              cap-y      (get-cap-dim cap-type :y)
                              placed-key (translate [x-offset y-offset 0]
                                                    (cube cap-x cap-y cap-height :center nil))]
                          (recur (rest row_)
                                 (+ cap-x (gap-d :x) x-offset) ;; Increase the offset by the size of the current cap the gap
                                 (conj row-caps placed-key))))))]

    (loop [kb-map_  kb-map
           y-offset 0
           all-keys []]
      (if (empty? kb-map_)
        all-keys ;; Exit loop if there are no more rows to process
        (recur (rest kb-map_)
               (+ (gap-d :y) (+ gap-offset u-y-d) y-offset)
               (conj all-keys (place-row (first kb-map_) y-offset)))))))

(defn all [is-right]
  (let [left-text (->> (text "Left" :font "Avenir" :size 30)
                       (extrude-linear { :height (+ cover-additional-z cap-height 0)})

                       (translate [10 20 (+ cover-additional-z cap-height 0)]))
        nb-top-key-in-u         (+ 14 1.5)
        nb-key-for-gaps 14
        original-gap-d  2
        u1-d            17
        right-text      (->> (text "Right" :font "Avenir" :size 30)
                             (extrude-linear { :height (+ cover-additional-z cap-height 0)})
                             (translate [30 20 (+ cover-additional-z cap-height 0)]))
        ;right-offset 136.8
        right-offset 137.8
        right-part      (difference
                         (union
                          right-thumb-cluster
                          (translate [right-offset 0 0] over))
                         (-# keyboard)
                         (-# right-speaker)
                         (translate [keyboard-x-d 0 0] speaker-holes)
                         (translate [(+ right-offset) 0 0] (-# right-text))
                         )
        left-part       (difference
                         (union left-thumb-cluster (translate [(- speaker-x-d) 0 0] over))
                         (-# keyboard)
                         left-speaker
                         (translate [( - speaker-x-d) 0 0] speaker-holes)
                         (-# left-text))]
    
    (mirror [0 0 1] (mirror [1 0 0] 
      (if is-right (union right-part) (union left-part))))))

(spit
 (str "resources/" "right.scad")
 (write-scad [
              (all true)
              (fn! scad-fn)]))

(spit
 (str "resources/" "left.scad")
 (write-scad [
              (all false)
              (fn! scad-fn)]))

;;;; Playground
(comment (let [mapp [[:u1 :u1point25] [:u1 :u1]]]
           (for [row mapp]
             (for [cap row]
               (cube (get-cap-dim cap :x) (get-cap-dim cap :y) 0)))))
(comment (let [mapp [[:u1 :u1point25] [:u1 :u1]]]
           (map (fn [i e1] (map-indexed (fn [j e2] (cube 0 0 0)) e1)) mapp)))

(comment (let [kb-map [[1 2]
                       [3 4]]]
           (loop [kb-map_ kb-map
                  y-offset 0
                  all-keys []]
             (let [row (first kb-map_)]
               (if (nil? row)
                 all-keys
                 (let [tmp (loop [row_ row
                                  x-offset 0
                                  row-caps []]
                             (let [cap (first row_)]
                               (if (nil? cap)
                                 row-caps
                                 (recur (rest row_)
                                        (+ 1 x-offset)
                                        (conj row-caps :cube)))))]
                   (recur (rest kb-map_)
                          (+ 1 y-offset)
                          (conj all-keys tmp))))))))
