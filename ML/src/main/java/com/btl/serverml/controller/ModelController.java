package com.btl.serverml.controller;

import com.btl.serverml.entity.ManagedModel;
import com.btl.serverml.repository.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/models")
public class ModelController {

    @Autowired
    private ModelRepository modelRepository;

    @GetMapping
    public List<ManagedModel> list() { return modelRepository.findAll(); }

    @PostMapping
    public ManagedModel create(@RequestBody ManagedModel m) { return modelRepository.save(m); }

    @PutMapping
    public ManagedModel update(@RequestBody ManagedModel m) { return modelRepository.save(m); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { modelRepository.deleteById(id); }
}
