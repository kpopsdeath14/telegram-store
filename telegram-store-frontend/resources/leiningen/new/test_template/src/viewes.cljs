(ns {{name}}.viewes
  (:require
   [reagent.core :as reagent]
   [{{name}}.db :refer [app-state]]
   [reagent.ratom :as ratom]
   [{{name}}.pages.catalog.catalog :refer [catalog_page]]
   [{{name}}.pages.product.product :refer [product_page]]
   [{{name}}.pages.information.information :refer [information_page]]
   [{{name}}.pages.cart.cart :refer [cart_page]]
   [{{name}}.pages.delivery.delivery :refer [delivery_page]]
   [{{name}}.pages.orders-history.orders-history :refer [orders_history_page]]
   [{{name}}.pages.order.order :refer [order_page]]
   [{{name}}.pages.favorites.favorites :refer [favorites_page]]
   [{{name}}.pages.thank-you.thank-you :refer [thank_you_page]]
   )
  )

(defmulti current-page #(@app-state :page))


(defmethod current-page :catalog []
  [catalog_page]
  )


(defmethod current-page :product []
  [product_page]
  )

(defmethod current-page :information []
  [information_page]
  )

(defmethod current-page :cart []
  [cart_page]
  )

(defmethod current-page :delivery []
  [delivery_page]
  )

(defmethod current-page :orders-history []
  [orders_history_page]
  )

(defmethod current-page :order []
  [order_page]
  )

(defmethod current-page :favorites []
  [favorites_page]
  )

(defmethod current-page :thank-you []
  [thank_you_page]
  )
