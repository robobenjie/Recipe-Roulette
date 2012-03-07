
window.display_search = ->

refresh = ->
   keyword_text = $('#main-input').val()
   keyword_text = keyword_text.replace ///[^a-zA-Z\'\s]///, ''
   keyword_text = keyword_text.replace ///\s+///g, '-'
   window.location.href = keyword_text

$(document).ready ->
  $('#make-recipe-btn').on "click", refresh
  $('#main-input').keyup (e) ->
    refresh() if e.which is 13
  $('#main-input').focus()
  display_search()
