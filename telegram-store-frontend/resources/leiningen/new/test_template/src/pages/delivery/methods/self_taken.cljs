(ns {{name}}.pages.delivery.methods.self-taken)

(defn self-taken-block [pickup-address]
  [:div {:style {:border "1px solid var(--color-border)"
                 :borderRadius 6
                 :padding "12px 14px"
                 :background "#fafafa"
                 :color "#111"}}
   [:div {:style {:fontSize 12
                  :color "#8c8c8c"
                  :marginBottom 6}}
    "Адрес самовывоза"]
   [:div {:style {:fontSize 16
                  :lineHeight "20px"}}
    (if (seq pickup-address) pickup-address "Адрес не указан")]])
