
Gerrit.install(function(self) {
    function onSchedule(c) {
        var t = c.textarea();
        var b = c.button('Build', {onclick: function() {
          c.call(
            {message: t.value},
            function(r) {
              c.hide();
              c.refresh();
            });
        }});
        c.popup(c.div(
          c.msg('The following platforms are going to be built:'),
          c.br(),
          c.msg('Mac OS X 32 bit'),
          c.br(),
          c.msg('Linux 32/64 bit'),
          c.br(),
          c.msg('Windows 32 bit'),
          c.br(),
          b));
        t.focus();
      }
      self.onAction('revision', 'schedule', onSchedule);
  });
