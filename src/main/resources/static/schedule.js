
Gerrit.install(function(self) {
    function onSchedule(c) {
        var t = c.textarea();
        var p1 = c.checkbox();
        var p2 = c.checkbox();
        var p3 = c.checkbox();
        var b = c.button('Build', {onclick: function() {
          c.call(
            {message: t.value},
            function(r) {
              c.hide();
              c.refresh();
            });
        }});
        c.popup(c.div(
          c.msg('Custom configuration options:'),
          c.br(),
          t,
          c.br(),
          c.msg('Optional platforms:'),
          c.br(),
          c.label(p1, 'Mac OS X 64 bit'),
          c.br(),
          c.label(p2, 'iOS 7.x'),
          c.br(),
          c.label(p3, 'Android 4.x'),
          c.br(),
          b));
        t.focus();
      }
      self.onAction('revision', 'schedule', onSchedule);
  });
